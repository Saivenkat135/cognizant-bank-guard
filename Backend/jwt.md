1) Filters in the Project
Filters live in two layers — server-side (security) and client-side (UI search).

Backend filters
JwtAuthenticationFilter — JwtAuthenticationFilter.java
A WebFilter registered in the reactive Spring Security chain. It runs on every request through the API Gateway and:

Skips /auth/login and /auth/register (public).
Reads the Authorization: Bearer <token> header — if missing → returns 401.
Validates the JWT via JwtUtil.isTokenValid(token).
Extracts username (subject) and role (custom claim).
Builds a UsernamePasswordAuthenticationToken with authority ROLE_<role> and writes it to the reactive security context with ReactiveSecurityContextHolder.withAuthentication(...).
This filter is registered at SecurityWebFiltersOrder.AUTHENTICATION in SecurityConfig.java:159 so it runs before Spring's authorization checks.

CORS filter
Configured in SecurityConfig.java:36-53 (gateway) and Backend/AlertCaseService/.../config/CorsConfig.java. Whitelists Vite dev ports 5173-5177, allows GET/POST/PUT/DELETE/PATCH/OPTIONS, and enables credentials.

Authorization rules (also filters, by path/role)
In SecurityConfig.java:68-156. Examples:

POST /api/transactions/** → SUPER_ADMIN only
GET /api/investigation/** → SUPER_ADMIN, FRAUD_ANALYST
/api/gemini/** → SUPER_ADMIN, RISK_MANAGER
Frontend filters (UI search / dropdowns)
These are client-side useMemo filters over data already fetched once:

File	Filters used
TransactionsTable.jsx:38-52	free-text search (id/customer/receiver), status dropdown, min/max amount
CustomersTable.jsx:35-46	search, account-type dropdown, bank dropdown (populated dynamically from data)
Reports.jsx:33-58	date-range from/to filter, then derives daily summary + flagged-only list
Users.jsx:42-54	search, status (pending/approved), role dropdown
ProtectedRoute.jsx:6-13	route-level role filter (allowedRoles)
Pattern: fetch once → keep raw rows in state → useMemo(() => rows.filter(...)) recomputes whenever a filter input changes.

2) API Integration
The frontend talks to the gateway through a single Axios instance.

Layers

Component  →  *.service.jsx  →  api.config.jsx (Axios)  →  API Gateway (:1001)  →  microservice
The Axios client — api.config.jsx

const api = axios.create({ baseURL: API_BASE_URL, headers: {'Content-Type':'application/json'}, timeout: 30000 });

// REQUEST interceptor: attaches JWT
api.interceptors.request.use((config) => {
  const token = localStorage.getItem(STORAGE_KEYS.TOKEN);
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// RESPONSE interceptor: on 401 → clear storage + redirect to /
api.interceptors.response.use(res => res, err => { ... });
Two interceptors do the heavy lifting:

Request interceptor stamps Authorization: Bearer <token> on every call → the component never has to think about auth headers.
Response interceptor auto-logs the user out on a 401 (token expired/invalid) by clearing localStorage and redirecting.
Service modules
Each domain has a service that returns a uniform { success, data | error } envelope:

auth.service.jsx — login, register, logout, user-management calls
transaction.service.jsx — getAllTransactions, getTransactionById, getTransactionsByCustomer
customer.service.jsx
rule.service.jsx
The wrap() helper normalizes errors so components only deal with result.success.

Component usage (typical)

useEffect(() => {
  (async () => {
    const r = await getAllTransactions();
    if (r.success) setRows(r.data);
    else setError(r.error);
    setLoading(false);
  })();
}, []);
Backend side — service-to-service
Microservices call each other through Feign / WebClient (e.g. enrichmentService → AlertCaseService via AlertCaseClient.java) and are discovered via Eureka, with the API Gateway as the single front door.

3) localStorage — Keys & Flow
Keys (defined once, used everywhere) — constants.jsx:26-30

export const STORAGE_KEYS = {
  TOKEN:    'token',     // JWT
  ROLE:     'role',      // SUPER_ADMIN | FRAUD_ANALYST | RISK_MANAGER
  USERNAME: 'username'
};
Defining them as constants prevents typos like "toekn" and gives one place to rename keys.

Write flow (login) — auth.service.jsx:19-30

User submits Login form
  ↓
AuthContext.login(credentials)
  ↓
auth.service.login() → POST /auth/login → gateway → AuthController.login()
  ↓
Backend returns { token, username, role, message }
  ↓
localStorage.setItem('token', token);
localStorage.setItem('role', role);
localStorage.setItem('username', username);
  ↓
AuthContext setIsLoggedIn(true), setRole(...), setUsername(...)
  ↓
RoleHome redirects to /admin/users, /risk/transactions, or /fraud/dashboard
Read flow (every subsequent request)
Component calls getAllTransactions().
Axios request interceptor in api.config.jsx:11-15 reads localStorage.getItem('token').
Appends Authorization: Bearer <token> header.
Backend filter validates it.
Read flow (on page refresh) — AuthContext.jsx:21-23

const [isLoggedIn, setIsLoggedIn] = useState(isAuthenticated());   // !!localStorage('token')
const [role, setRole]             = useState(getRole());           // localStorage('role')
const [username, setUsername]     = useState(getUsername());       // localStorage('username')
React state is seeded from localStorage — that's why a refresh doesn't bounce you to /login.

Clear flow
Manual logout — auth.service.jsx:35-40: removeItem for each of the 3 keys + redirect to /.
Auto logout on 401 — api.config.jsx:18-28: localStorage.clear() + window.location.href = '/'.
Visual

┌─────────────┐        login        ┌──────────────┐
│   Browser   │ ──────────────────▶ │ API Gateway  │
│             │                     │ AuthController│
│ localStorage│ ◀── { token,role } ─│              │
│  token      │                     └──────────────┘
│  role       │
│  username   │      every subsequent call:
│             │ ── Authorization: Bearer <token> ──▶ Gateway
└─────────────┘
4) JWT Flow (end-to-end)
Step 1 — Login (token issued)
AuthController.java:86-122


POST /auth/login { username, password }
  ↓
Find user in DB → bcrypt-compare password
  ↓
Check user.isApproved (or role == SUPER_ADMIN)
  ↓
jwtUtil.generateToken(username, role)
  ↓
Return { token, username, role, message }
Step 2 — Token generation
JwtUtil.java:27-38


Jwts.builder()
    .claims(Map.of("role", role))
    .subject(username)
    .issuedAt(new Date())
    .expiration(new Date(now + jwt.expiration))
    .signWith(HMAC-SHA key from jwt.secret)
    .compact();
Resulting JWT shape:


HEADER  { alg:HS256, typ:JWT }
PAYLOAD { sub:<username>, role:<ROLE>, iat:..., exp:... }
SIGNATURE  HMAC-SHA(base64(header).base64(payload), secret)
Step 3 — Frontend stores & re-attaches
auth.service.login saves it to localStorage.token.
Every api.* call → request interceptor adds Authorization: Bearer <token>.
Step 4 — Gateway validates on every request
JwtAuthenticationFilter.java:27-69


Request hits gateway
  ↓
Is path /auth/login or /auth/register? → pass through
  ↓
Read "Authorization: Bearer <token>"; if missing → 401
  ↓
JwtUtil.isTokenValid(token):
    - parse signed claims using same secret
    - check expiration date
  ↓
extract username (sub) + role
  ↓
build Authentication with authority "ROLE_<role>"
  ↓
write into ReactiveSecurityContextHolder
  ↓
Spring's authorizeExchange() rules (SecurityConfig) check role vs path
  ↓
Forward to downstream microservice  OR  return 403/401
Step 5 — Token expiry / failure
extractAllClaims throws → isTokenValid returns false → filter writes 401.
Browser's Axios response interceptor sees 401 → clears storage → routes to /login.
Step 6 — Logout
Pure client-side: remove token/role/username from localStorage and navigate to /. The JWT itself is stateless — the server doesn't track sessions, so removing it from the browser is enough.

Visual

   ┌─────────┐  POST /auth/login                ┌──────────────┐
   │ Browser │ ───────────────────────────────▶ │ AuthController│
   │         │                                  │  + JwtUtil    │
   │         │ ◀───── { token, role } ───────── │ (signs JWT)   │
   └────┬────┘                                  └──────────────┘
        │ save → localStorage(token, role, username)
        │
        │ subsequent calls:
        │  Authorization: Bearer <jwt>
        ▼
   ┌──────────────────────┐
   │ API Gateway          │
   │  JwtAuthenticationFilter  ──► validate sig + exp
   │  SecurityConfig           ──► role vs path check
   └─────────┬────────────┘
             ▼ (if authorised)
   ┌──────────────────────┐
   │ Microservice (Tx/Cust/Alert/Sar/Enrich/Gemini)
   └──────────────────────┘
Summary table
Concern	Where it lives
Issue JWT	JwtUtil.generateToken called by AuthController.login
Store JWT	auth.service.jsx:22 → localStorage
Send JWT	api.config.jsx:11-15 request interceptor
Verify JWT	JwtAuthenticationFilter
Role-based access	SecurityConfig.authorizeExchange + ProtectedRoute.jsx
Logout / token expiry	auth.service.logout + api.config response interceptor