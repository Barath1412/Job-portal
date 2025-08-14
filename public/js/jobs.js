document.addEventListener('DOMContentLoaded', function() {
    const searchInput = document.querySelector('input[type="text"]');
    const jobsContainer = document.querySelector('.grid');

    // Fetch jobs from API
    fetch('/api/jobs')
        .then(response => response.json())
        .then(jobs => {
            displayJobs(jobs);
            
            // Search functionality
            searchInput.addEventListener('input', (e) => {
                const searchTerm = e.target.value.toLowerCase();
                const filteredJobs = jobs.filter(job => 
                    job.location.toLowerCase().includes(searchTerm)
                );
                displayJobs(filteredJobs);
            });
        });

    function displayJobs(jobs) {
        jobsContainer.innerHTML = '';
        jobs.forEach(job => {
            const jobCard = document.createElement('div');
            jobCard.className = 'bg-white p-4 rounded shadow hover:shadow-lg transition-shadow';
            jobCard.innerHTML = `
                <h3 class="text-xl font-bold">${job.title}</h3>
                <p class="text-gray-600">${job.company}</p>
                <p class="text-gray-500">${job.location}</p>
                <p class="text-sm text-gray-400">Deadline: ${new Date(job.deadline).toLocaleDateString()}</p>
                <a href="apply.html?jobId=${job.id}" class="mt-2 inline-block bg-blue-500 text-white px-3 py-1 rounded hover:bg-blue-600">
                    Apply Now
                </a>
            `;
            jobsContainer.appendChild(jobCard);
        });
    }
});