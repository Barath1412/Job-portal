document.addEventListener('DOMContentLoaded', function() {
    if (!requireAuth(['CANDIDATE'])) return;

    const applicationsTable = document.getElementById('applicationsTable');
    const noAppsMsg = document.getElementById('noAppsMsg');
    const errorAlert = document.getElementById('errorAlert');

    authFetch('/api/applications/me')
        .then(response => {
            if (!response.ok) throw new Error('Failed to load applications');
            return response.json();
        })
        .then(applications => {
            if (!applications || applications.length === 0) {
                noAppsMsg.classList.remove('hidden');
                return;
            }

            applicationsTable.innerHTML = '';
            applications.forEach(app => {
                const tr = document.createElement('tr');

                const jobTitle = app.job ? app.job.title : 'Job #' + app.jobId;
                const company = app.job ? app.job.company : 'N/A';
                const location = app.job ? app.job.location : 'N/A';
                const status = app.status || 'PENDING';

                const badgeClass = getStatusBadgeClass(status);

                tr.innerHTML = `
                    <td class="px-6 py-4 whitespace-nowrap font-medium text-gray-900">${escapeHtml(jobTitle)}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${escapeHtml(company)}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">📍 ${escapeHtml(location)}</td>
                    <td class="px-6 py-4 whitespace-nowrap">
                        <span class="px-2.5 py-1 text-xs font-semibold rounded-full border ${badgeClass}">
                            ${escapeHtml(status)}
                        </span>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm">
                        ${app.resumeUrl ? `<a href="${escapeHtml(app.resumeUrl)}" target="_blank" rel="noopener" class="text-blue-600 hover:underline font-medium">View Resume ↗</a>` : '<span class="text-gray-400">None</span>'}
                    </td>
                `;
                applicationsTable.appendChild(tr);
            });
        })
        .catch(err => {
            console.error(err);
            errorAlert.textContent = err.message;
            errorAlert.classList.remove('hidden');
        });
});

function getStatusBadgeClass(status) {
    switch (status) {
        case 'PENDING':
            return 'bg-yellow-100 text-yellow-800 border-yellow-200';
        case 'REVIEWED':
            return 'bg-blue-100 text-blue-800 border-blue-200';
        case 'INTERVIEW':
            return 'bg-purple-100 text-purple-800 border-purple-200';
        case 'OFFERED':
            return 'bg-green-100 text-green-800 border-green-200';
        case 'REJECTED':
            return 'bg-red-100 text-red-800 border-red-200';
        default:
            return 'bg-gray-100 text-gray-800 border-gray-200';
    }
}
