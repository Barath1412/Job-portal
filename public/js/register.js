document.addEventListener('DOMContentLoaded', function() {
    const registerForm = document.getElementById('registerForm');
    const errorAlert = document.getElementById('error-alert');

    // If user is already logged in, redirect away
    const user = getUser();
    if (user) {
        if (user.role === 'CANDIDATE') window.location.href = 'jobs.html';
        else if (user.role === 'RECRUITER' || user.role === 'ADMIN') window.location.href = 'admin.html';
    }

    registerForm.addEventListener('submit', function(e) {
        e.preventDefault();
        errorAlert.classList.add('hidden');
        errorAlert.textContent = '';

        const name = registerForm.name.value.trim();
        const email = registerForm.email.value.trim();
        const password = registerForm.password.value;
        const role = registerForm.role.value;

        if (role !== 'CANDIDATE' && role !== 'RECRUITER') {
            errorAlert.textContent = 'Invalid role selected.';
            errorAlert.classList.remove('hidden');
            return;
        }

        authFetch('/api/auth/register', {
            method: 'POST',
            body: JSON.stringify({ name, email, password, role })
        })
        .then(async response => {
            const data = await response.json();
            if (!response.ok) {
                throw new Error(data.error || 'Registration failed. Please try again.');
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
