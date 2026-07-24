// Auth helper utility for Job Portal

function saveSession(token, user) {
    localStorage.setItem('jp_token', token);
    localStorage.setItem('jp_user', JSON.stringify(user));
}

function getToken() {
    return localStorage.getItem('jp_token');
}

function getUser() {
    const userStr = localStorage.getItem('jp_user');
    try {
        return userStr ? JSON.parse(userStr) : null;
    } catch (e) {
        return null;
    }
}

function clearSession() {
    localStorage.removeItem('jp_token');
    localStorage.removeItem('jp_user');
}

function authFetch(url, options = {}) {
    options.headers = options.headers || {};
    const token = getToken();
    if (token) {
        options.headers['Authorization'] = 'Bearer ' + token;
    }
    if (options.body && typeof options.body === 'string' && !options.headers['Content-Type']) {
        options.headers['Content-Type'] = 'application/json';
    }
    return fetch(url, options);
}

function authFetchMultipart(url, formData) {
    const options = {
        method: 'POST',
        headers: {},
        body: formData
    };
    const token = getToken();
    if (token) {
        options.headers['Authorization'] = 'Bearer ' + token;
    }
    return fetch(url, options);
}

function requireAuth(allowedRoles) {
    const token = getToken();
    const user = getUser();
    if (!token || !user) {
        clearSession();
        window.location.href = 'login.html';
        return false;
    }
    if (allowedRoles && Array.isArray(allowedRoles) && !allowedRoles.includes(user.role)) {
        if (user.role === 'CANDIDATE') {
            window.location.href = 'jobs.html';
        } else if (user.role === 'RECRUITER') {
            window.location.href = 'admin.html';
        } else {
            window.location.href = 'index.html';
        }
        return false;
    }
    return true;
}

function renderNavAuthState() {
    const navContainer = document.getElementById('nav-header');
    if (!navContainer) return;

    const user = getUser();
    let authNavHtml = '';

    if (user) {
        let dashboardLink = '';
        if (user.role === 'CANDIDATE') {
            dashboardLink = `<a href="my-applications.html" class="text-gray-700 hover:text-blue-600 px-3 py-2 font-medium">My Applications</a>`;
        } else if (user.role === 'RECRUITER' || user.role === 'ADMIN') {
            dashboardLink = `<a href="admin.html" class="text-gray-700 hover:text-blue-600 px-3 py-2 font-medium">Dashboard</a>`;
        }

        authNavHtml = `
            <div class="flex items-center space-x-4">
                ${dashboardLink}
                <span class="text-sm font-semibold text-gray-700 bg-gray-100 px-3 py-1.5 rounded-full border border-gray-200">
                    👋 ${escapeHtml(user.name)} (${escapeHtml(user.role)})
                </span>
                <button onclick="handleLogout()" class="bg-red-500 text-white hover:bg-red-600 px-4 py-2 rounded-md font-medium text-sm transition">
                    Logout
                </button>
            </div>
        `;
    } else {
        authNavHtml = `
            <div class="flex items-center space-x-3">
                <a href="login.html" class="text-gray-700 hover:text-blue-600 px-4 py-2 font-medium border border-gray-300 rounded-md">Log in</a>
                <a href="register.html" class="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-md font-medium transition">Register</a>
            </div>
        `;
    }

    navContainer.innerHTML = `
        <nav class="bg-white border-b border-gray-200 shadow-sm">
            <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                <div class="flex justify-between h-16">
                    <div class="flex items-center space-x-8">
                        <a href="index.html" class="text-2xl font-bold text-blue-600 tracking-tight">JobPortal</a>
                        <a href="jobs.html" class="text-gray-700 hover:text-blue-600 px-3 py-2 font-medium">Find Jobs</a>
                    </div>
                    <div class="flex items-center">
                        ${authNavHtml}
                    </div>
                </div>
            </div>
        </nav>
    `;
}

function handleLogout() {
    clearSession();
    window.location.href = 'index.html';
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/[&<>"']/g, function (m) {
        return {
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#039;'
        }[m];
    });
}

document.addEventListener('DOMContentLoaded', function () {
    renderNavAuthState();
});
