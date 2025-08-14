document.addEventListener('DOMContentLoaded', function() {
    const addJobForm = document.getElementById('addJobForm');
    const applicationsTable = document.getElementById('applicationsTable');

    // Load applications
    fetch('/api/applications', {
        headers: {
            'Authorization': 'Basic ' + btoa('admin:admin123')
        }
    })
    .then(response => response.json())
    .then(applications => {
        applications.forEach(app => {
            const row = document.createElement('tr');
            row.className = 'border';
            row.innerHTML = `
                <td class="py-2 px-4 border">${app.applicantName}</td>
                <td class="py-2 px-4 border">${app.email}</td>
                <td class="py-2 px-4 border">
                    <a href="#" class="text-blue-500 hover:underline">View Resume</a>
                </td>
                <td class="py-2 px-4 border">
                    <button class="text-red-500 hover:text-red-700" onclick="deleteApplication('${app.id}')">
                        Delete
                    </button>
                </td>
            `;
            applicationsTable.appendChild(row);
        });
    });

    // Add job form submission
    addJobForm.addEventListener('submit', function(e) {
        e.preventDefault();
        
        const job = {
            title: addJobForm.elements[0].value,
            company: addJobForm.elements[1].value,
            location: addJobForm.elements[2].value,
            description: addJobForm.elements[3].value,
            deadline: addJobForm.elements[4].value
        };

        fetch('/api/jobs', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Basic ' + btoa('admin:admin123')
            },
            body: JSON.stringify(job)
        })
        .then(response => response.json())
        .then(data => {
            alert('Job added successfully!');
            addJobForm.reset();
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Error adding job. Please try again.');
        });
    });
});

function deleteApplication(id) {
    if (confirm('Are you sure you want to delete this application?')) {
        fetch(`/api/applications/${id}`, {
            method: 'DELETE',
            headers: {
                'Authorization': 'Basic ' + btoa('admin:admin123')
            }
        })
        .then(() => {
            alert('Application deleted successfully!');
            location.reload();
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Error deleting application.');
        });
    }
}