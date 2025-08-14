document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('applicationForm');
    const urlParams = new URLSearchParams(window.location.search);
    const jobId = urlParams.get('jobId');

    form.addEventListener('submit', function(e) {
        e.preventDefault();
        
        const formData = new FormData(form);
        const application = {
            jobId: jobId,
            applicantName: formData.get('name'),
            email: formData.get('email'),
            coverLetter: formData.get('coverLetter')
        };

        // In a real app, you would upload the resume file first
        // and then include the URL in the application object
        
        fetch('/api/applications', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(application)
        })
        .then(response => response.json())
        .then(data => {
            alert('Application submitted successfully!');
            form.reset();
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Error submitting application. Please try again.');
        });
    });
});