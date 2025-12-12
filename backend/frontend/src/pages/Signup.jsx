import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { TrendingUp, ArrowLeft, Eye, EyeOff } from 'lucide-react';
import './Signup.css';

const Signup = () => {
    const navigate = useNavigate();
    const [formData, setFormData] = useState({
        email: '',
        firstName: '',
        lastName: '',
        password: '',
        confirmPassword: '',
        phoneNumber: ''
    });
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);
    const [errors, setErrors] = useState({});
    const [isLoading, setIsLoading] = useState(false);
    const [successMessage, setSuccessMessage] = useState('');

    const validateForm = () => {
        const newErrors = {};

        // Email validation
        if (!formData.email) {
            newErrors.email = 'Email is required';
        } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
            newErrors.email = 'Please provide a valid email';
        }

        // First Name validation
        if (!formData.firstName) {
            newErrors.firstName = 'First Name is required';
        } else if (formData.firstName.length < 2 || formData.firstName.length > 100) {
            newErrors.firstName = 'First Name must be between 2 and 100 characters';
        }

        // Last Name validation
        if (!formData.lastName) {
            newErrors.lastName = 'Last Name is required';
        } else if (formData.lastName.length < 2 || formData.lastName.length > 100) {
            newErrors.lastName = 'Last Name must be between 2 and 100 characters';
        }

        // Password validation
        if (!formData.password) {
            newErrors.password = 'Password is required';
        } else if (formData.password.length < 6 || formData.password.length > 100) {
            newErrors.password = 'Password must be between 6 and 100 characters';
        }

        // Confirm Password validation
        if (!formData.confirmPassword) {
            newErrors.confirmPassword = 'Please confirm your password';
        } else if (formData.password !== formData.confirmPassword) {
            newErrors.confirmPassword = 'Passwords do not match';
        }

        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
        // Clear error for this field when user starts typing
        if (errors[name]) {
            setErrors(prev => ({
                ...prev,
                [name]: ''
            }));
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!validateForm()) {
            return;
        }

        setIsLoading(true);
        setErrors({});
        setSuccessMessage('');

        try {
            const response = await fetch('http://localhost:8090/api/users/register', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-Request-ID': crypto.randomUUID()
                },
                body: JSON.stringify({
                    email: formData.email,
                    firstName: formData.firstName,
                    lastName: formData.lastName,
                    password: formData.password,
                    phoneNumber: formData.phoneNumber || null
                })
            });

            const data = await response.json();

            if (response.ok) {
                setSuccessMessage('Account created successfully! Redirecting to login...');
                setTimeout(() => {
                    navigate('/login');
                }, 2000);
            } else {
                // Handle validation errors from backend
                if (data.errors) {
                    const backendErrors = {};
                    data.errors.forEach(error => {
                        backendErrors[error.field] = error.message;
                    });
                    setErrors(backendErrors);
                } else {
                    setErrors({ general: data.message || 'Registration failed. Please try again.' });
                }
            }
        } catch (error) {
            console.error('Registration error:', error);
            setErrors({ general: 'Network error. Please check your connection and try again.' });
        } finally {
            setIsLoading(false);
        }
    };

    const handleGoogleSignup = () => {
        window.location.href = "http://localhost:8090/oauth2/authorization/google";
    };

    return (
        <div className="signup-container">
            <div className="signup-content glass">
                <a href="/" className="back-link">
                    <ArrowLeft size={20} />
                    Back to Home
                </a>

                <div className="signup-header">
                    <div className="logo-icon-lg">
                        <TrendingUp className="text-white" size={32} />
                    </div>
                    <h1 className="signup-title">Create Your Account</h1>
                    <p className="signup-subtitle">Start your investment journey today</p>
                </div>

                {errors.general && (
                    <div className="error-banner">
                        {errors.general}
                    </div>
                )}

                {successMessage && (
                    <div className="success-banner">
                        {successMessage}
                    </div>
                )}

                <div className="signup-actions">
                    <button className="google-signup-btn" onClick={handleGoogleSignup}>
                        <svg className="google-icon" viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg">
                            <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z" />
                            <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z" />
                            <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z" />
                            <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z" />
                            <path fill="none" d="M0 0h48v48H0z" />
                        </svg>
                        <span>Continue with Google</span>
                    </button>

                    <div className="divider-text">or</div>

                    <form className="signup-form" onSubmit={handleSubmit}>
                        <div className="form-row">
                            <div className="form-group">
                                <label>First Name <span className="required">*</span></label>
                                <input
                                    type="text"
                                    name="firstName"
                                    placeholder="John"
                                    className={`form-input ${errors.firstName ? 'error' : ''}`}
                                    value={formData.firstName}
                                    onChange={handleChange}
                                    disabled={isLoading}
                                />
                                {errors.firstName && <span className="error-text">{errors.firstName}</span>}
                            </div>
                            <div className="form-group">
                                <label>Last Name <span className="required">*</span></label>
                                <input
                                    type="text"
                                    name="lastName"
                                    placeholder="Doe"
                                    className={`form-input ${errors.lastName ? 'error' : ''}`}
                                    value={formData.lastName}
                                    onChange={handleChange}
                                    disabled={isLoading}
                                />
                                {errors.lastName && <span className="error-text">{errors.lastName}</span>}
                            </div>
                        </div>

                        <div className="form-group">
                            <label>Email Address <span className="required">*</span></label>
                            <input
                                type="email"
                                name="email"
                                placeholder="name@example.com"
                                className={`form-input ${errors.email ? 'error' : ''}`}
                                value={formData.email}
                                onChange={handleChange}
                                disabled={isLoading}
                            />
                            {errors.email && <span className="error-text">{errors.email}</span>}
                        </div>

                        <div className="form-group">
                            <label>Phone Number <span className="optional">(Optional)</span></label>
                            <input
                                type="tel"
                                name="phoneNumber"
                                placeholder="+1 (555) 000-0000"
                                className="form-input"
                                value={formData.phoneNumber}
                                onChange={handleChange}
                                disabled={isLoading}
                            />
                        </div>

                        <div className="form-group">
                            <label>Password <span className="required">*</span></label>
                            <div className="password-input-wrapper">
                                <input
                                    type={showPassword ? "text" : "password"}
                                    name="password"
                                    placeholder="Enter your password (min. 6 characters)"
                                    className={`form-input ${errors.password ? 'error' : ''}`}
                                    value={formData.password}
                                    onChange={handleChange}
                                    disabled={isLoading}
                                />
                                <button
                                    type="button"
                                    className="password-toggle"
                                    onClick={() => setShowPassword(!showPassword)}
                                    disabled={isLoading}
                                >
                                    {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                                </button>
                            </div>
                            {errors.password && <span className="error-text">{errors.password}</span>}
                        </div>

                        <div className="form-group">
                            <label>Confirm Password <span className="required">*</span></label>
                            <div className="password-input-wrapper">
                                <input
                                    type={showConfirmPassword ? "text" : "password"}
                                    name="confirmPassword"
                                    placeholder="Re-enter your password"
                                    className={`form-input ${errors.confirmPassword ? 'error' : ''}`}
                                    value={formData.confirmPassword}
                                    onChange={handleChange}
                                    disabled={isLoading}
                                />
                                <button
                                    type="button"
                                    className="password-toggle"
                                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                                    disabled={isLoading}
                                >
                                    {showConfirmPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                                </button>
                            </div>
                            {errors.confirmPassword && <span className="error-text">{errors.confirmPassword}</span>}
                        </div>

                        <button
                            className="btn-primary w-full"
                            type="submit"
                            disabled={isLoading}
                        >
                            {isLoading ? 'Creating Account...' : 'Create Account'}
                        </button>
                    </form>

                    <p className="login-text">
                        Already have an account? <a href="/login" className="text-primary">Sign in</a>
                    </p>
                </div>
            </div>

            {/* Background Elements */}
            <div className="signup-glow glow-1"></div>
            <div className="signup-glow glow-2"></div>
            <div className="signup-glow glow-3"></div>
        </div>
    );
};

export default Signup;
