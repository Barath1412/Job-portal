document.addEventListener('DOMContentLoaded', function() {
    if (!requireAuth(['RECRUITER', 'ADMIN'])) return;

    const createCompanyForm = document.getElementById('createCompanyForm');
    const createCompanyAlert = document.getElementById('createCompanyAlert');
    const createCompanyBtn = document.getElementById('createCompanyBtn');

    const addJobForm = document.getElementById('addJobForm');
    const postJobAlert = document.getElementById('postJobAlert');
    const postJobBtn = document.getElementById('postJobBtn');
    const noCompanyPrompt = document.getElementById('noCompanyPrompt');
    const jobCompanySelect = document.getElementById('jobCompany');

    const myCompaniesList = document.getElementById('myCompaniesList');
    const noCompaniesNotice = document.getElementById('noCompaniesNotice');

    const postedJobsList = document.getElementById('postedJobsList');
    const noJobsNotice = document.getElementById('noJobsNotice');

    const currentUser = getUser();

    // Initial loads
    loadMyCompanies();
    loadMyJobs();

    // Company Creation Form Submit
    if (createCompanyForm) {
        createCompanyForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            createCompanyAlert.classList.add('hidden');
            createCompanyAlert.textContent = '';

            const name = createCompanyForm.name.value.trim();
            const industry = createCompanyForm.industry.value.trim();
            const website = createCompanyForm.website.value.trim();
            const description = createCompanyForm.description.value.trim();
            const logoFileInput = document.getElementById('companyLogoFile');

            if (!name) {
                createCompanyAlert.textContent = 'Company name is required.';
                createCompanyAlert.classList.remove('hidden');
                return;
            }

            const originalBtnText = createCompanyBtn.textContent;
            createCompanyBtn.disabled = true;
            createCompanyBtn.textContent = 'Creating company...';

            let logoUrl = null;
            try {
                if (logoFileInput && logoFileInput.files && logoFileInput.files.length > 0) {
                    const file = logoFileInput.files[0];
                    const allowedTypes = ['image/png', 'image/jpeg', 'image/jpg', 'image/webp'];
                    if (!allowedTypes.includes(file.type.toLowerCase())) {
                        throw new Error('Only image files (PNG, JPG, WEBP) are allowed for logos.');
                    }

                    createCompanyBtn.textContent = 'Uploading logo...';
                    const formData = new FormData();
                    formData.append('file', file);

                    const uploadRes = await authFetchMultipart('/api/upload/logo', formData);
                    const uploadData = await uploadRes.json();
                    if (!uploadRes.ok) {
                        throw new Error(uploadData.error || 'Failed to upload logo.');
                    }
                    logoUrl = uploadData.url;
                }

                createCompanyBtn.textContent = 'Saving company...';
                const companyPayload = { name, description, website, industry, logoUrl };

                const res = await authFetch('/api/companies', {
                    method: 'POST',
                    body: JSON.stringify(companyPayload)
                });
                const data = await res.json();
                if (!res.ok) {
                    throw new Error(data.error || 'Failed to create company profile.');
                }

                showToast('Company profile created successfully! 🏢', 'success');
                createCompanyForm.reset();
                loadMyCompanies();

            } catch (err) {
                console.error('Create company error:', err);
                createCompanyAlert.textContent = err.message;
                createCompanyAlert.classList.remove('hidden');
            } finally {
                createCompanyBtn.disabled = false;
                createCompanyBtn.textContent = originalBtnText;
            }
        });
    }

    // Add Job Form Submit
    if (addJobForm) {
        addJobForm.addEventListener('submit', function(e) {
            e.preventDefault();
            postJobAlert.classList.add('hidden');
            postJobAlert.textContent = '';

            const companyId = Number(addJobForm.companyId.value);
            if (!companyId) {
                postJobAlert.textContent = 'Please select a company.';
                postJobAlert.classList.remove('hidden');
                return;
            }

            const jobData = {
                companyId: companyId,
                title: addJobForm.title.value.trim(),
                location: addJobForm.location.value.trim(),
                description: addJobForm.description.value.trim(),
                deadline: addJobForm.deadline.value
            };

            authFetch('/api/jobs', {
                method: 'POST',
                body: JSON.stringify(jobData)
            })
            .then(async response => {
                const data = await response.json();
                if (!response.ok) {
                    throw new Error(data.error || 'Failed to post job.');
                }
                return data;
            })
            .then(newJob => {
                showToast('Job posted successfully! 🎉', 'success');
                addJobForm.reset();
                loadMyJobs();
            })
            .catch(error => {
                console.error('Error posting job:', error);
                postJobAlert.textContent = error.message;
                postJobAlert.classList.remove('hidden');
            });
        });
    }

    function loadMyCompanies() {
        authFetch('/api/companies/mine')
            .then(res => {
                if (!res.ok) throw new Error('Failed to load companies');
                return res.json();
            })
            .then(companies => {
                myCompaniesList.innerHTML = '';
                jobCompanySelect.innerHTML = '<option value="">Select a Company</option>';

                if (!companies || companies.length === 0) {
                    noCompaniesNotice.classList.remove('hidden');
                    noCompanyPrompt.classList.remove('hidden');
                    if (postJobBtn) postJobBtn.disabled = true;
                    return;
                }

                noCompaniesNotice.classList.add('hidden');
                noCompanyPrompt.classList.add('hidden');
                if (postJobBtn) postJobBtn.disabled = false;

                companies.forEach(company => {
                    // Populate select dropdown
                    const opt = document.createElement('option');
                    opt.value = company.id;
                    opt.textContent = company.name;
                    jobCompanySelect.appendChild(opt);

                    // Render company card
                    const card = document.createElement('div');
                    card.className = 'p-4 border border-gray-200 rounded-lg bg-gray-50 flex items-center justify-between shadow-sm';
                    
                    const logoHtml = company.logoUrl 
                        ? `<img src="${escapeHtml(company.logoUrl)}" alt="Logo" class="w-10 h-10 object-cover rounded-md border border-gray-200">`
                        : `<div class="w-10 h-10 bg-blue-100 text-blue-600 rounded-md flex items-center justify-center font-bold text-sm">🏢</div>`;

                    card.innerHTML = `
                        <div class="flex items-center space-x-3">
                            ${logoHtml}
                            <div>
                                <h4 class="font-bold text-gray-900 text-sm">${escapeHtml(company.name)}</h4>
                                <p class="text-xs text-gray-500">${escapeHtml(company.industry || 'Industry N/A')}</p>
                            </div>
                        </div>
                        <button onclick="deleteCompany(${company.id}, '${escapeJsString(company.name)}')" 
                                class="text-red-500 hover:text-red-700 text-xs font-semibold px-2 py-1 rounded border border-red-200 hover:bg-red-50 transition">
                            Delete
                        </button>
                    `;
                    myCompaniesList.appendChild(card);
                });
            })
            .catch(err => {
                console.error('Error loading recruiter companies:', err);
            });
    }

    function loadMyJobs() {
        authFetch('/api/jobs')
            .then(res => {
                if (!res.ok) throw new Error('Failed to load jobs');
                return res.json();
            })
            .then(data => {
                postedJobsList.innerHTML = '';
                const jobs = data.content || data;
                
                // Filter jobs posted by current user (unless ADMIN)
                const myJobs = (currentUser && currentUser.role === 'ADMIN') 
                    ? jobs 
                    : jobs.filter(j => j.postedBy && j.postedBy.id === currentUser.id);

                if (!myJobs || myJobs.length === 0) {
                    noJobsNotice.classList.remove('hidden');
                    return;
                }
                noJobsNotice.classList.add('hidden');

                myJobs.forEach(job => {
                    const card = document.createElement('div');
                    card.className = 'p-4 border border-gray-200 rounded-lg hover:border-blue-300 transition bg-gray-50 flex justify-between items-center';
                    
                    const deadlineText = job.deadline ? new Date(job.deadline).toLocaleDateString() : 'N/A';
                    const companyName = job.company ? job.company.name : (job.companyNameLegacy || 'Company N/A');

                    card.innerHTML = `
                        <div>
                            <h3 class="font-bold text-gray-900 text-lg">${escapeHtml(job.title)}</h3>
                            <p class="text-sm text-gray-600">${escapeHtml(companyName)} &bull; ${escapeHtml(job.location)}</p>
                            <p class="text-xs text-gray-400 mt-1">Deadline: ${deadlineText}</p>
                        </div>
                        <div>
                            <button onclick="viewApplicants(${job.id}, '${escapeJsString(job.title)}')" 
                                    class="bg-blue-600 hover:bg-blue-700 text-white px-3.5 py-1.5 rounded-md font-medium text-sm transition">
                                View Applicants
                            </button>
                        </div>
                    `;
                    postedJobsList.appendChild(card);
                });
            })
            .catch(err => {
                console.error('Error loading recruiter jobs:', err);
                postedJobsList.innerHTML = `<p class="text-red-600 text-sm">Error loading your posted jobs.</p>`;
            });
    }

    // Expose deleteCompany to window scope
    window.deleteCompany = function(companyId, companyName) {
        if (!confirm(`Are you sure you want to delete "${companyName}"?`)) return;

        authFetch(`/api/companies/${companyId}`, {
            method: 'DELETE'
        })
        .then(async res => {
            const data = await res.json();
            if (!res.ok) {
                throw new Error(data.error || 'Failed to delete company');
            }
            return data;
        })
        .then(data => {
            showToast(`Company deleted successfully!`, 'success');
            loadMyCompanies();
        })
        .catch(err => {
            console.error('Delete company error:', err);
            showToast(err.message, 'error');
        });
    };
});

// View applicants for a specific job
function viewApplicants(jobId, jobTitle) {
    const applicantsSection = document.getElementById('applicantsSection');
    const applicantsTitle = document.getElementById('applicantsTitle');
    const applicationsTable = document.getElementById('applicationsTable');
    const noApplicantsMsg = document.getElementById('noApplicantsMsg');
    const errorAlert = document.getElementById('applicantsErrorAlert');

    errorAlert.classList.add('hidden');
    applicantsSection.classList.remove('hidden');
    applicantsTitle.textContent = `Applicants for: ${jobTitle}`;
    applicationsTable.innerHTML = '';
    noApplicantsMsg.classList.add('hidden');

    applicantsSection.scrollIntoView({ behavior: 'smooth' });

    authFetch(`/api/jobs/${jobId}/applications`)
        .then(async res => {
            const data = await res.json();
            if (!res.ok) {
                throw new Error(data.error || 'Failed to fetch applicants');
            }
            return data;
        })
        .then(applications => {
            if (!applications || applications.length === 0) {
                noApplicantsMsg.classList.remove('hidden');
                return;
            }

            applications.forEach(app => {
                const tr = document.createElement('tr');

                const statusOptions = ['PENDING', 'REVIEWED', 'INTERVIEW', 'OFFERED', 'REJECTED'];
                const optionsHtml = statusOptions.map(opt => 
                    `<option value="${opt}" ${app.status === opt ? 'selected' : ''}>${opt}</option>`
                ).join('');

                const resumeLink = app.resumeUrl 
                    ? `<a href="${escapeHtml(app.resumeUrl)}" target="_blank" rel="noopener" class="text-blue-600 hover:underline font-medium">View Resume ↗</a>` 
                    : '<span class="text-gray-400">None</span>';

                tr.innerHTML = `
                    <td class="px-4 py-3 whitespace-nowrap text-sm font-medium text-gray-900">${escapeHtml(app.applicantName || (app.applicant ? app.applicant.name : 'Unknown'))}</td>
                    <td class="px-4 py-3 whitespace-nowrap text-sm text-gray-500">${escapeHtml(app.email || (app.applicant ? app.applicant.email : 'N/A'))}</td>
                    <td class="px-4 py-3 whitespace-nowrap text-sm">${resumeLink}</td>
                    <td class="px-4 py-3 text-sm text-gray-600 max-w-xs truncate" title="${escapeHtml(app.coverLetter)}">${escapeHtml(app.coverLetter || '-')}</td>
                    <td class="px-4 py-3 whitespace-nowrap">
                        <select onchange="updateStatus(${app.id}, this.value)"
                                class="text-xs font-semibold rounded-md border border-gray-300 p-1.5 focus:ring-blue-500 focus:border-blue-500 bg-white">
                            ${optionsHtml}
                        </select>
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
}

function updateStatus(appId, newStatus) {
    authFetch(`/api/applications/${appId}/status`, {
        method: 'PATCH',
        body: JSON.stringify({ status: newStatus })
    })
    .then(async res => {
        const data = await res.json();
        if (!res.ok) {
            throw new Error(data.error || 'Failed to update status');
        }
        return data;
    })
    .then(updatedApp => {
        showToast(`Application status updated to ${newStatus}!`, 'success');
    })
    .catch(err => {
        console.error('Error updating status:', err);
        showToast(`Failed to update status: ${err.message}`, 'error');
    });
}

function closeApplicantsSection() {
    const applicantsSection = document.getElementById('applicantsSection');
    if (applicantsSection) applicantsSection.classList.add('hidden');
}

function showToast(message, type = 'info') {
    const toast = document.getElementById('toastNotification');
    const toastMsg = document.getElementById('toastMessage');
    if (!toast || !toastMsg) return;

    toastMsg.textContent = message;
    if (type === 'error') {
        toast.className = "fixed top-20 right-5 z-50 max-w-sm w-full bg-red-50 text-red-800 shadow-lg rounded-lg border border-red-200 p-4 flex items-center justify-between";
    } else {
        toast.className = "fixed top-20 right-5 z-50 max-w-sm w-full bg-green-50 text-green-800 shadow-lg rounded-lg border border-green-200 p-4 flex items-center justify-between";
    }

    toast.classList.remove('hidden');
    setTimeout(() => {
        hideToast();
    }, 4000);
}

function hideToast() {
    const toast = document.getElementById('toastNotification');
    if (toast) toast.classList.add('hidden');
}

function escapeJsString(str) {
    if (!str) return '';
    return str.replace(/'/g, "\\'");
}