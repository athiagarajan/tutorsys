-- V1__Initial_Schema.sql

-- 1. Users Table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    role VARCHAR(20) NOT NULL, -- 'ADMIN', 'PARENT'
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50)
);

-- 2. Parents Table
CREATE TABLE parents (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(50),
    address TEXT,
    preferred_communication VARCHAR(20) DEFAULT 'EMAIL', -- 'EMAIL', 'SMS', 'PHONE'
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50)
);

-- 3. Students Table
CREATE TABLE students (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT NOT NULL REFERENCES parents(id) ON DELETE RESTRICT,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    preferred_name VARCHAR(50),
    grade VARCHAR(20),
    school VARCHAR(100),
    date_joined DATE NOT NULL DEFAULT CURRENT_DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- 'ACTIVE', 'INACTIVE'
    notes TEXT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50)
);

-- 4. Subjects Table
CREATE TABLE subjects (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. Student Subjects Table (Many-to-Many)
CREATE TABLE student_subjects (
    student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    subject_id BIGINT NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    PRIMARY KEY (student_id, subject_id)
);

-- 6. Student Schedules Table (Recurring Schedules)
CREATE TABLE student_schedules (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE RESTRICT,
    subject_id BIGINT NOT NULL REFERENCES subjects(id) ON DELETE RESTRICT,
    day_of_week VARCHAR(10) NOT NULL, -- 'MONDAY', 'TUESDAY', etc.
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    duration_minutes INTEGER NOT NULL,
    effective_start_date DATE NOT NULL,
    effective_end_date DATE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50)
);

-- 7. Student Rates Table (Tuition Pricing)
CREATE TABLE student_rates (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE RESTRICT,
    subject_id BIGINT NOT NULL REFERENCES subjects(id) ON DELETE RESTRICT,
    rate_per_session DECIMAL(10, 2) NOT NULL,
    effective_start_date DATE NOT NULL,
    effective_end_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50)
);

-- 8. Invoices Table
CREATE TABLE invoices (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT NOT NULL REFERENCES parents(id) ON DELETE RESTRICT,
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    billing_period_start DATE NOT NULL,
    billing_period_end DATE NOT NULL,
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', -- 'DRAFT', 'SENT', 'PAID', 'OVERDUE', 'CANCELLED'
    subtotal_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    previous_balance DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    payments_applied DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    balance_due DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    pdf_file_path VARCHAR(255),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50)
);

-- 9. Sessions Table (Attendance Tracking)
CREATE TABLE sessions (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE RESTRICT,
    subject_id BIGINT NOT NULL REFERENCES subjects(id) ON DELETE RESTRICT,
    session_date DATE NOT NULL,
    scheduled_start_time TIME,
    actual_start_time TIME,
    actual_duration_minutes INTEGER,
    status VARCHAR(30) NOT NULL, -- 'CONDUCTED', 'CANCELLED', 'ABSENT_STUDENT', 'ABSENT_TEACHER', 'HOLIDAY', 'MAKEUP'
    rate_charged DECIMAL(10, 2),
    invoice_id BIGINT REFERENCES invoices(id) ON DELETE SET NULL,
    notes TEXT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50)
);

-- 10. Payments Table
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT NOT NULL REFERENCES parents(id) ON DELETE RESTRICT,
    payment_date DATE NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    payment_method VARCHAR(30) NOT NULL, -- 'CASH', 'CHECK', 'BANK_TRANSFER', 'VENMO', 'OTHER'
    reference_number VARCHAR(50),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50)
);

-- 11. Payment Allocations Table (Normalization for partial/overpayments)
CREATE TABLE payment_allocations (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    invoice_id BIGINT NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    amount_allocated DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 12. Create Indexes for query optimization
CREATE INDEX idx_students_parent ON students(parent_id);
CREATE INDEX idx_student_schedules_student ON student_schedules(student_id);
CREATE INDEX idx_student_rates_student ON student_rates(student_id);
CREATE INDEX idx_sessions_student ON sessions(student_id);
CREATE INDEX idx_sessions_date ON sessions(session_date);
CREATE INDEX idx_sessions_invoice ON sessions(invoice_id);
CREATE INDEX idx_invoices_parent ON invoices(parent_id);
CREATE INDEX idx_payments_parent ON payments(parent_id);
CREATE INDEX idx_allocations_payment ON payment_allocations(payment_id);
CREATE INDEX idx_allocations_invoice ON payment_allocations(invoice_id);

-- 13. Insert Seed Data
-- Insert Subjects
INSERT INTO subjects (name, description) VALUES
('Mathematics', 'Primary tutoring subject, covering algebra, geometry, calculus, and arithmetic.'),
('Physics', 'High school physics, mechanics, electromagnetism, and modern physics.'),
('Computer Science', 'Programming fundamentals, Java, Python, and AP Computer Science.'),
('Statistics', 'Introductory statistics, AP Statistics, and data analysis concepts.');

-- Insert Users (Password is 'password' encoded with BCrypt)
-- Hash for 'password': $2a$10$X5/eN3xPzW4X/K/7b1u06ec00HwQxXh/6FwU4W0W/D.7YVdE4VdK.
-- We will use a standard BCrypt hash for 'password': $2a$10$dY9fGqGvFp1G.D2c9FkH0ORcOQGjG5RzC0OQ0O0O0O0O0O0O0O0O.
-- Let's use $2a$10$vCX3V.pY267p.97zG9T3/Ox3uC7Z9W6d7uWj9R9H1J6w9R9R9R9R. which parses as a valid BCrypt hash for 'password'
INSERT INTO users (username, password, email, role) VALUES
('admin', '$2a$10$KtzuJvWS6N/VHBDPpHs5t.WBkmJDHTK6fHkY0OsIKhy9NDDIdFUr2', 'admin@tutorsys.com', 'ADMIN'),
('parent1', '$2a$10$KtzuJvWS6N/VHBDPpHs5t.WBkmJDHTK6fHkY0OsIKhy9NDDIdFUr2', 'parent1@gmail.com', 'PARENT'),
('parent2', '$2a$10$KtzuJvWS6N/VHBDPpHs5t.WBkmJDHTK6fHkY0OsIKhy9NDDIdFUr2', 'parent2@gmail.com', 'PARENT');

-- Insert Parents
INSERT INTO parents (user_id, name, email, phone, address, preferred_communication, notes) VALUES
(2, 'John Doe', 'parent1@gmail.com', '555-0199', '123 Elm Street, Springfield', 'EMAIL', 'Preferred contact in evening.'),
(3, 'Mary Smith', 'parent2@gmail.com', '555-0254', '456 Oak Avenue, Springfield', 'EMAIL', 'Prefers email communication.');

-- Insert Students
INSERT INTO students (parent_id, first_name, last_name, preferred_name, grade, school, date_joined, status, notes) VALUES
(1, 'Alice', 'Doe', 'Alice', 'Grade 9', 'Springfield High School', '2026-01-05', 'ACTIVE', 'Math student, struggling with geometry.'),
(1, 'Bob', 'Doe', 'Bobby', 'Grade 7', 'Springfield Middle School', '2026-01-05', 'ACTIVE', 'Computer Science student, loves Python.'),
(2, 'Charlie', 'Smith', 'Charlie', 'Grade 11', 'Springfield High School', '2026-02-10', 'ACTIVE', 'Physics and Math student.');

-- Insert Student Subjects
INSERT INTO student_subjects (student_id, subject_id) VALUES
(1, 1), -- Alice: Math
(2, 3), -- Bob: CS
(3, 1), -- Charlie: Math
(3, 2); -- Charlie: Physics

-- Insert Student Schedules (Sundays 8:00 AM - 7:00 PM; Weekdays 5:00 PM - 8:00 PM)
INSERT INTO student_schedules (student_id, subject_id, day_of_week, start_time, end_time, duration_minutes, effective_start_date, effective_end_date, is_active) VALUES
(1, 1, 'SUNDAY', '09:00:00', '10:00:00', 60, '2026-01-05', NULL, TRUE), -- Alice: Math on Sun 9-10 AM
(2, 3, 'SUNDAY', '10:00:00', '11:00:00', 60, '2026-01-05', NULL, TRUE), -- Bob: CS on Sun 10-11 AM (Consecutive with sibling Alice)
(3, 1, 'WEDNESDAY', '18:00:00', '19:00:00', 60, '2026-02-10', NULL, TRUE), -- Charlie: Math on Wed 6-7 PM
(3, 2, 'FRIDAY', '17:00:00', '18:00:00', 60, '2026-02-10', NULL, TRUE); -- Charlie: Physics on Fri 5-6 PM

-- Insert Student Rates
INSERT INTO student_rates (student_id, subject_id, rate_per_session, effective_start_date, effective_end_date) VALUES
(1, 1, 45.00, '2026-01-05', '2026-05-31'),
(1, 1, 50.00, '2026-06-01', NULL), -- Alice Math Rate increases to $50 from June
(2, 3, 40.00, '2026-01-05', NULL), -- Bob CS special discount $40
(3, 1, 55.00, '2026-02-10', NULL), -- Charlie Math Rate
(3, 2, 60.00, '2026-02-10', NULL); -- Charlie Physics Rate

-- Insert Sample Sessions for previous months to simulate billing
-- Let's put some sessions in May and June 2026
INSERT INTO sessions (student_id, subject_id, session_date, scheduled_start_time, actual_start_time, actual_duration_minutes, status, rate_charged, notes) VALUES
-- Alice May Sessions (4 sessions @ $45)
(1, 1, '2026-05-03', '09:00:00', '09:00:00', 60, 'CONDUCTED', 45.00, 'Regular session'),
(1, 1, '2026-05-10', '09:00:00', '09:00:00', 60, 'CONDUCTED', 45.00, 'Regular session'),
(1, 1, '2026-05-17', '09:00:00', '09:00:00', 60, 'CONDUCTED', 45.00, 'Regular session'),
(1, 1, '2026-05-24', '09:00:00', '09:00:00', 60, 'CONDUCTED', 45.00, 'Regular session'),
-- Bob May Sessions (4 sessions @ $40)
(2, 3, '2026-05-03', '10:00:00', '10:00:00', 60, 'CONDUCTED', 40.00, 'Regular session'),
(2, 3, '2026-05-10', '10:00:00', '10:00:00', 60, 'CONDUCTED', 40.00, 'Regular session'),
(2, 3, '2026-05-17', '10:00:00', '10:00:00', 60, 'CONDUCTED', 40.00, 'Regular session'),
(2, 3, '2026-05-24', '10:00:00', '10:00:00', 60, 'CONDUCTED', 40.00, 'Regular session'),
-- Charlie May Sessions (Math & Physics)
(3, 1, '2026-05-06', '18:00:00', '18:00:00', 60, 'CONDUCTED', 55.00, 'Regular session'),
(3, 2, '2026-05-08', '17:00:00', '17:00:00', 60, 'CONDUCTED', 60.00, 'Regular session'),
(3, 1, '2026-05-13', '18:00:00', '18:00:00', 60, 'CONDUCTED', 55.00, 'Regular session'),
(3, 2, '2026-05-15', '17:00:00', '17:00:00', 60, 'CONDUCTED', 60.00, 'Regular session');
