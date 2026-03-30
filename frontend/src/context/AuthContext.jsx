import React, { createContext, useState, useEffect } from 'react';
import api from '../services/api';

export const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const checkLoggedIn = () => {
            const token = localStorage.getItem('token') || sessionStorage.getItem('token');
            const userData = localStorage.getItem('user') || sessionStorage.getItem('user');
            
            if (token && userData) {
                setUser(JSON.parse(userData));
            }
            setLoading(false);
        };
        checkLoggedIn();
    }, []);

    const login = async (identifier, password, rememberMe = false) => {
        const response = await api.post('/auth/login', { identifier, password });
        // Assuming the backend returns the token field (JwtResponse returns 'token', actually wait the original code used response.data.accessToken or response.data.token)
        const token = response.data.accessToken || response.data.token;
        if (token) {
            const storage = rememberMe ? localStorage : sessionStorage;
            storage.setItem('token', token);
            storage.setItem('user', JSON.stringify(response.data));
            setUser(response.data);
            return response.data;
        }
        return null;
    };

    const register = async (userData) => {
        return await api.post('/auth/register', userData);
    };

    const logout = () => {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        sessionStorage.removeItem('token');
        sessionStorage.removeItem('user');
        setUser(null);
    };

    return (
        <AuthContext.Provider value={{ user, login, register, logout, loading }}>
            {!loading && children}
        </AuthContext.Provider>
    );
};
