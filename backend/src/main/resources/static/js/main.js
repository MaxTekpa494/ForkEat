// Menu mobile toggle
document.addEventListener('DOMContentLoaded', function() {
    const mobileMenuButton = document.getElementById('mobile-menu-button');
    const mobileMenu = document.getElementById('mobile-menu');

    if (mobileMenuButton && mobileMenu) {
        mobileMenuButton.addEventListener('click', function() {
            mobileMenu.classList.toggle('hidden');

            // Changer l'icône
            const icon = mobileMenuButton.querySelector('i');
            if (mobileMenu.classList.contains('hidden')) {
                icon.classList.remove('fa-times');
                icon.classList.add('fa-bars');
            } else {
                icon.classList.remove('fa-bars');
                icon.classList.add('fa-times');
            }
        });
    }

    // Auto-fermeture des messages flash après 5 secondes
    setTimeout(() => {
        const alerts = document.querySelectorAll('[th\\:if="${success}"], [th\\:if="${error}"]');
        alerts.forEach(alert => {
            alert.style.opacity = '0';
            alert.style.transition = 'opacity 0.5s';
            setTimeout(() => alert.remove(), 500);
        });
    }, 5000);
});

const userMenuButton = document.getElementById('user-menu-button');
const userMenuDropdown = document.getElementById('user-menu-dropdown');
const userMenuChevron = document.getElementById('user-menu-chevron');

if (userMenuButton && userMenuDropdown) {
    userMenuButton.addEventListener('click', function(e) {
        e.stopPropagation();
        userMenuDropdown.classList.toggle('hidden');
        if (userMenuChevron) {
            userMenuChevron.classList.toggle('rotate-180');
        }
    });

    // Fermer si clic ailleurs
    document.addEventListener('click', function(e) {
        if (!userMenuButton.contains(e.target) && !userMenuDropdown.contains(e.target)) {
            userMenuDropdown.classList.add('hidden');
            if (userMenuChevron) {
                userMenuChevron.classList.remove('rotate-180');
            }
        }
    });
}

// Smooth scroll pour les ancres
document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener('click', function (e) {
        e.preventDefault();
        const target = document.querySelector(this.getAttribute('href'));
        if (target) {
            target.scrollIntoView({
                behavior: 'smooth',
                block: 'start'
            });
        }
    });
});