document.addEventListener('DOMContentLoaded', function() {
    // DOM Elements
    const titleFilter = document.getElementById('titleFilter');
    const locationFilter = document.getElementById('locationFilter');
    const companyFilter = document.getElementById('companyFilter');
    const sortSelect = document.getElementById('sortSelect');
    const clearFiltersBtn = document.getElementById('clearFiltersBtn');
    const clearFiltersBtnEmpty = document.getElementById('clearFiltersBtnEmpty');
    const retryBtn = document.getElementById('retryBtn');

    const loadingState = document.getElementById('loadingState');
    const errorState = document.getElementById('errorState');
    const errorMessage = document.getElementById('errorMessage');
    const jobsContainer = document.getElementById('jobsContainer');
    const noJobsMsg = document.getElementById('noJobsMsg');
    
    const paginationControls = document.getElementById('paginationControls');
    const prevPageBtn = document.getElementById('prevPageBtn');
    const nextPageBtn = document.getElementById('nextPageBtn');
    const pageIndicator = document.getElementById('pageIndicator');
    const resultsCount = document.getElementById('resultsCount');

    // Current Query State
    let currentPage = 0;
    const pageSize = 10;
    let totalPages = 0;

    // Read initial state from URL query parameters
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('title') && titleFilter) titleFilter.value = urlParams.get('title');
    if (urlParams.has('location') && locationFilter) locationFilter.value = urlParams.get('location');
    if (urlParams.has('company') && companyFilter) companyFilter.value = urlParams.get('company');
    if (urlParams.has('page')) currentPage = parseInt(urlParams.get('page')) || 0;

    if (urlParams.has('sortBy') && sortSelect) {
        const sb = urlParams.get('sortBy');
        const sd = urlParams.get('sortDir') || 'asc';
        const val = `${sb}:${sd}`;
        for (let opt of sortSelect.options) {
            if (opt.value === val) {
                sortSelect.value = val;
                break;
            }
        }
    }

    // Debounce Helper (400ms)
    function debounce(func, delay = 400) {
        let timer;
        return function(...args) {
            clearTimeout(timer);
            timer = setTimeout(() => func.apply(this, args), delay);
        };
    }

    const debouncedFilter = debounce(() => {
        currentPage = 0; // Reset to first page on filter change
        fetchJobs();
    }, 400);

    // Event Listeners for Filter Inputs
    if (titleFilter) titleFilter.addEventListener('input', debouncedFilter);
    if (locationFilter) locationFilter.addEventListener('input', debouncedFilter);
    if (companyFilter) companyFilter.addEventListener('input', debouncedFilter);

    if (sortSelect) {
        sortSelect.addEventListener('change', () => {
            currentPage = 0;
            fetchJobs();
        });
    }

    if (clearFiltersBtn) clearFiltersBtn.addEventListener('click', clearAllFilters);
    if (clearFiltersBtnEmpty) clearFiltersBtnEmpty.addEventListener('click', clearAllFilters);
    if (retryBtn) retryBtn.addEventListener('click', fetchJobs);

    if (prevPageBtn) {
        prevPageBtn.addEventListener('click', () => {
            if (currentPage > 0) {
                currentPage--;
                fetchJobs();
            }
        });
    }

    if (nextPageBtn) {
        nextPageBtn.addEventListener('click', () => {
            if (currentPage < totalPages - 1) {
                currentPage++;
                fetchJobs();
            }
        });
    }

    function clearAllFilters() {
        if (titleFilter) titleFilter.value = '';
        if (locationFilter) locationFilter.value = '';
        if (companyFilter) companyFilter.value = '';
        if (sortSelect) sortSelect.value = 'deadline:asc';
        currentPage = 0;
        fetchJobs();
    }

    function syncUrlParams(title, location, company, sortBy, sortDir) {
        const params = new URLSearchParams();
        if (title) params.set('title', title);
        if (location) params.set('location', location);
        if (company) params.set('company', company);
        if (sortBy) params.set('sortBy', sortBy);
        if (sortDir) params.set('sortDir', sortDir);
        if (currentPage > 0) params.set('page', currentPage);

        const newUrl = window.location.pathname + (params.toString() ? '?' + params.toString() : '');
        window.history.replaceState(null, '', newUrl);
    }

    // Core Fetch Function
    function fetchJobs() {
        const title = titleFilter ? titleFilter.value.trim() : '';
        const location = locationFilter ? locationFilter.value.trim() : '';
        const company = companyFilter ? companyFilter.value.trim() : '';
        
        let sortBy = 'deadline';
        let sortDir = 'asc';
        if (sortSelect && sortSelect.value) {
            const parts = sortSelect.value.split(':');
            sortBy = parts[0] || 'deadline';
            sortDir = parts[1] || 'asc';
        }

        syncUrlParams(title, location, company, sortBy, sortDir);

        // UI States - Loading
        loadingState.classList.remove('hidden');
        errorState.classList.add('hidden');
        jobsContainer.classList.add('hidden');
        noJobsMsg.classList.add('hidden');
        paginationControls.classList.add('hidden');
        resultsCount.textContent = 'Searching...';

        const queryParams = new URLSearchParams({
            page: currentPage,
            size: pageSize,
            sortBy: sortBy,
            sortDir: sortDir
        });

        if (title) queryParams.set('title', title);
        if (location) queryParams.set('location', location);
        if (company) queryParams.set('company', company);

        authFetch(`/api/jobs?${queryParams.toString()}`)
            .then(async response => {
                if (!response.ok) {
                    const errData = await response.json().catch(() => ({}));
                    throw new Error(errData.error || `HTTP Error ${response.status}`);
                }
                return response.json();
            })
            .then(data => {
                loadingState.classList.add('hidden');

                const jobs = data.content || [];
                const totalElements = data.totalElements || 0;
                totalPages = data.totalPages || 0;
                currentPage = data.currentPage || 0;

                resultsCount.textContent = `Showing ${jobs.length} of ${totalElements} total job(s)`;

                if (jobs.length === 0) {
                    noJobsMsg.classList.remove('hidden');
                    return;
                }

                displayJobs(jobs);
                updatePaginationControls();
                jobsContainer.classList.remove('hidden');
                paginationControls.classList.remove('hidden');
            })
            .catch(err => {
                console.error('Fetch jobs error:', err);
                loadingState.classList.add('hidden');
                errorMessage.textContent = err.message || 'Failed to load jobs.';
                errorState.classList.remove('hidden');
                resultsCount.textContent = 'Error loading jobs';
            });
    }

    function updatePaginationControls() {
        pageIndicator.textContent = `Page ${currentPage + 1} of ${Math.max(1, totalPages)}`;
        prevPageBtn.disabled = (currentPage === 0);
        nextPageBtn.disabled = (currentPage >= totalPages - 1 || totalPages === 0);
    }

    function displayJobs(jobs) {
        jobsContainer.innerHTML = '';
        const currentUser = getUser();

        jobs.forEach(job => {
            const card = document.createElement('div');
            card.className = 'bg-white p-6 rounded-lg shadow-sm border border-gray-200 hover:shadow-md transition flex flex-col justify-between';

            let actionBtn = '';
            if (currentUser && (currentUser.role === 'RECRUITER' || currentUser.role === 'ADMIN')) {
                actionBtn = `<a href="admin.html" class="inline-block bg-gray-100 text-gray-700 px-4 py-2 rounded-md font-medium text-sm hover:bg-gray-200 text-center">Manage Jobs</a>`;
            } else {
                actionBtn = `<a href="apply.html?jobId=${job.id}" class="inline-block bg-blue-600 text-white px-4 py-2 rounded-md font-medium text-sm hover:bg-blue-700 text-center transition">Apply Now</a>`;
            }

            const companyName = job.company ? job.company.name : (job.companyNameLegacy || 'Company N/A');
            const logoUrl = job.company ? job.company.logoUrl : null;
            const industryText = (job.company && job.company.industry) ? ` &bull; ${escapeHtml(job.company.industry)}` : '';

            const logoHtml = logoUrl 
                ? `<img src="${escapeHtml(logoUrl)}" alt="${escapeHtml(companyName)} Logo" class="w-10 h-10 object-cover rounded-md border border-gray-200 shadow-sm flex-shrink-0">`
                : `<div class="w-10 h-10 bg-blue-100 text-blue-600 rounded-md flex items-center justify-center font-bold text-sm flex-shrink-0">🏢</div>`;

            const postedByText = job.postedBy ? escapeHtml(job.postedBy.name) : 'Employer';
            const deadlineText = job.deadline ? new Date(job.deadline).toLocaleDateString() : 'N/A';

            card.innerHTML = `
                <div>
                    <div class="flex items-start space-x-3 mb-3">
                        ${logoHtml}
                        <div class="flex-grow">
                            <h3 class="text-lg font-bold text-gray-900 leading-snug">${escapeHtml(job.title)}</h3>
                            <div class="flex items-center space-x-2 mt-0.5">
                                <span class="text-sm font-semibold text-blue-700">${escapeHtml(companyName)}</span>
                                <span class="text-xs text-gray-400">📍 ${escapeHtml(job.location)}</span>
                            </div>
                        </div>
                    </div>
                    <p class="text-xs text-gray-400 mb-3">Posted by: ${postedByText}${industryText}</p>
                    <p class="text-gray-700 text-sm mb-4 line-clamp-3">${escapeHtml(job.description)}</p>
                </div>
                <div class="pt-4 border-t border-gray-100 flex items-center justify-between">
                    <span class="text-xs text-gray-400">Deadline: ${deadlineText}</span>
                    ${actionBtn}
                </div>
            `;
            jobsContainer.appendChild(card);
        });
    }

    // Initial Fetch
    fetchJobs();
});