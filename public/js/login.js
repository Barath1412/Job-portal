document.addEventListener('DOMContentLoaded', function() {
    const loginForm = document.getElementById('loginForm');
    const errorAlert = document.getElementById('error-alert');

    // If user is already logged in, redirect away
    const user = getUser();
    if (user) {
        if (user.role === 'CANDIDATE') window.location.href = 'jobs.html';
        else if (user.role === 'RECRUITER' || user.role === 'ADMIN') window.location.href = 'admin.html';
    }

    loginForm.addEventListener('submit', function(e) {
        e.preventDefault();
        errorAlert.classList.add('hidden');
        errorAlert.textContent = '';

        const email = loginForm.email.value.trim();
        const password = loginForm.password.value;

        authFetch('/api/auth/login', {
            method: 'POST',
            body: JSON.stringify({ email, password })
        })
        .then(async response => {
            const data = await response.json();
            if (!response.ok) {
                if (response.status === 401) {
                    throw new Error('Invalid email or password');
                }
                throw new Error(data.error || 'Login failed. Please check your credentials.');
            }
            return data;
        })
        .then(data => {
            saveSession(data.token, {
                id: data.id,
                name: data.name,
                email: data.email,
                role: data.role
            });

            if (data.role === 'CANDIDATE') {
                window.location.href = 'jobs.html';
            } else if (data.role === 'RECRUITER' || data.role === 'ADMIN') {
                window.location.href = 'admin.html';
            } else {
                window.location.href = 'index.html';
            }
        })
        .catch(err => {
            errorAlert.textContent = err.message;
            errorAlert.classList.remove('hidden');
        });
    });
});
