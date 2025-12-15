# Manager Login Details

## Default Manager Account

To create a default manager account, you can use the following methods:

### Method 1: Using API Endpoint (Recommended)

**Endpoint:** `POST /api/admins/create-default-manager`

**Request:**
```bash
curl -X POST http://localhost:8080/api/admins/create-default-manager
```

**Response:**
```json
{
  "success": true,
  "message": "Default manager account created successfully",
  "loginDetails": {
    "email": "manager@neobank.com",
    "password": "manager123",
    "role": "MANAGER"
  }
}
```

### Method 2: Create Manager via Admin Creation API

**Endpoint:** `POST /api/admins/create`

**Request Body:**
```json
{
  "name": "Manager",
  "email": "manager@neobank.com",
  "password": "manager123",
  "role": "MANAGER",
  "employeeId": "MGR001"
}
```

### Method 3: Direct Database Insert

If you have database access, you can insert directly:

```sql
INSERT INTO admins (name, email, password, role, employee_id, created_at, last_updated)
VALUES (
  'Manager',
  'manager@neobank.com',
  '$2a$10$...', -- Encrypted password (use PasswordService to encrypt)
  'MANAGER',
  'MGR001',
  NOW(),
  NOW()
);
```

## Default Manager Credentials

**Email:** `manager@neobank.com`  
**Password:** `manager123`  
**Role:** `MANAGER`

⚠️ **Important:** Change the default password after first login for security!

## How to Login as Manager

1. Navigate to: `http://localhost:4200/admin/login`
2. Select **"Manager"** from the "Access Role" dropdown
3. Enter:
   - Email: `manager@neobank.com`
   - Password: `manager123`
4. Click "Login"
5. You will be redirected to the Manager Dashboard

## Manager Dashboard Features

Once logged in as manager, you can:
- **View Dashboard:** See statistics and quick actions
- **Feature Access Control:** Enable/disable admin features
- **Admin Management:** View and manage admin accounts (coming soon)

## Creating Additional Manager Accounts

You can create additional manager accounts using the same API endpoint with different credentials:

```json
{
  "name": "Senior Manager",
  "email": "senior.manager@neobank.com",
  "password": "your-secure-password",
  "role": "MANAGER",
  "employeeId": "MGR002"
}
```

## Security Notes

- Always use strong passwords in production
- Change default passwords immediately
- Consider implementing password policies
- Use HTTPS in production environments

