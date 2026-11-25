window.addEventListener('load', function () {
    const dropdownToggle = document.getElementById('notificationDropdown');

    if (!dropdownToggle) {
        console.log("❌ Notification bell not found!");
        return;
    }

    console.log("✅ Notification bell found, event listener attached.");

    // Click event
    dropdownToggle.addEventListener('click', function () {
        console.log("🔔 Bell icon clicked!");
    });

    // Bootstrap dropdown event
    dropdownToggle.addEventListener('shown.bs.dropdown', async function () {
        console.log("📬 Notification dropdown opened!");

        const badge = dropdownToggle.querySelector('span.badge');
        if (!badge) {
            console.log("No unread notifications to mark as read.");
            return;
        }

        const count = parseInt(badge.textContent.trim());
        if (count > 0) {
            console.log("Sending mark-read request...");

            try {
                const response = await fetch('/api/notifications/mark-read', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' }
                });

                if (response.ok) {
                    badge.remove();
                    console.log("✅ Notifications marked as read!");
                } else {
                    console.error("❌ Failed to mark read. Status:", response.status);
                }
            } catch (err) {
                console.error("⚠️ Error marking notifications:", err);
            }
        }
    });
});
