document.addEventListener('DOMContentLoaded', function() {
    // Require candidate authentication
    if (!requireAuth(['CANDIDATE'])) return;

    const form = document.getElementById('applicationForm');
    const errorAlert = document.getElementById('error-alert');
    const successAlert = document.getElementById('success-alert');
    const jobTitleHeader = document.getElementById('jobTitleHeader');
    const submitBtn = document.getElementById('submitBtn');

    const urlParams = new URLSearchParams(window.location.search);
    const jobId = urlParams.get('jobId');

    const user = getUser();
    if (user && form) {
        if (form.applicantName) form.applicantName.value = user.name || '';
        if (form.email) form.email.value = user.email || '';
    }

    if (!jobId) {
        errorAlert.textContent = 'Invalid job selection. Please select a job from the listings page.';
        errorAlert.classList.remove('hidden');
        if (form) form.classList.add('hidden');
        return;
    }

    // Fetch job details to display job title
    authFetch(`/api/jobs/${jobId}`)
        .then(response => {
            if (response.ok) return response.json();
            return null;
        })
        .then(job => {
            if (job && jobTitleHeader) {
                jobTitleHeader.textContent = `Applying for: ${job.title} at ${job.company}`;
            }
        })
        .catch(err => console.error('Error fetching job detail:', err));

    form.addEventListener('submit', async function(e) {
        e.preventDefault();
        errorAlert.classList.add('hidden');
        errorAlert.textContent = '';

        const fileInput = document.getElementById('resumeFile');
        if (!fileInput || !fileInput.files || fileInput.files.length === 0) {
            errorAlert.textContent = 'Please select a PDF resume file to upload.';
            errorAlert.classList.remove('hidden');
            return;
        }

        const file = fileInput.files[0];

        // Client-side Pre-validation
        const isPdf = file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf');
        if (!isPdf) {
            errorAlert.textContent = 'Only PDF files are allowed (.pdf).';
            errorAlert.classList.remove('hidden');
            return;
        }

        const maxSizeBytes = 5 * 1024 * 1024; // 5MB
        if (file.size > maxSizeBytes) {
            errorAlert.textContent = 'File size exceeds maximum limit of 5MB.';
            errorAlert.classList.remove('hidden');
            return;
        }

        // Show uploading state
        const originalBtnText = submitBtn.textContent;
        submitBtn.disabled = true;
        submitBtn.textContent = 'Uploading resume...';

        try {
            // Step 1: Upload File
            const formData = new FormData();
            formData.append('file', file);

            const uploadResponse = await authFetchMultipart('/api/upload/resume', formData);
            const uploadData = await uploadResponse.json();

            if (!uploadResponse.ok) {
                throw new Error(uploadData.error || 'Failed to upload resume.');
            }

            const resumeUrl = uploadData.url;

            // Step 2: Submit Application
            submitBtn.textContent = 'Submitting application...';
            const application = {
                jobId: Number(jobId),
                applicantName: form.applicantName.value.trim(),
                email: form.email.value.trim(),
                resumeUrl: resumeUrl,
                coverLetter: form.coverLetter.value.trim()
            };

            const appResponse = await authFetch('/api/applications', {
                method: 'POST',
                body: JSON.stringify(application)
            });
            const appData = await appResponse.json();

            if (!appResponse.ok) {
                if (appResponse.status === 409) {
                    throw new Error("You've already applied to this job");
                }
                throw new Error(appData.error || 'Failed to submit application.');
            }

            form.classList.add('hidden');
            successAlert.classList.remove('hidden');

        } catch (error) {
            errorAlert.textContent = error.message;
            errorAlert.classList.remove('hidden');
            submitBtn.disabled = false;
            submitBtn.textContent = originalBtnText;
        }
    });
});