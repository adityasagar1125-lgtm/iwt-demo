// ===== Silicon Club Hub — Shared JS =====
// Context path prefix for servlet calls
const CTX = (() => {
  const parts = window.location.pathname.split('/').filter(Boolean);
  return parts.length ? `/${parts[0]}` : '';
})();

function apiUrl(path) {
  return `${CTX}${path}`;
}

// ── API helpers ──────────────────────────────────────────
async function fetchClubs() {
  try {
    const res = await fetch(apiUrl('/api/clubs'));
    return await res.json();
  } catch (e) {
    console.error('Failed to load clubs', e);
    return [];
  }
}

async function fetchEvents(params = '') {
  try {
    const res = await fetch(apiUrl(`/api/events${params}`));
    return await res.json();
  } catch (e) {
    console.error('Failed to load events', e);
    return [];
  }
}

async function createClub(payload) {
  const res = await fetch(apiUrl('/api/clubs'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  return await res.json();
}

async function updateClub(payload) {
  const res = await fetch(apiUrl('/api/clubs'), {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  return await res.json();
}

async function deleteClub(id) {
  const res = await fetch(apiUrl(`/api/clubs?id=${id}`), { method: 'DELETE' });
  return await res.json();
}

async function createEvent(payload) {
  const res = await fetch(apiUrl('/api/events'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  return await res.json();
}

async function updateEvent(payload) {
  const res = await fetch(apiUrl('/api/events'), {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  return await res.json();
}

async function deleteEvent(id) {
  const res = await fetch(apiUrl(`/api/events?id=${id}`), { method: 'DELETE' });
  return await res.json();
}

// ── Date formatter ────────────────────────────────────────
function formatDate(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  return d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
}

// ── Club Card HTML ────────────────────────────────────────
function clubCardHTML(club) {
  return `
    <a class="club-card" href="club-detail.html?id=${club.id}" style="--accent:${club.colorAccent};">
      <div class="club-card-top">
        <div class="club-icon">${club.icon}</div>
        <span class="club-category-tag">${club.category}</span>
      </div>
      <div class="club-card-name">${club.name}</div>
      <div class="club-card-desc">${club.description}</div>
      <div class="club-card-footer">
        <span class="club-members">👥 ${club.memberCount}+ members</span>
        <div class="club-arrow">→</div>
      </div>
    </a>
  `;
}

// ── Event Card HTML ───────────────────────────────────────
function eventCardHTML(event) {
  const statusClass = {
    upcoming: 'status-upcoming',
    past: 'status-past',
    ongoing: 'status-ongoing'
  }[event.status] || 'status-upcoming';

  return `
    <div class="event-card">
      <div class="event-card-body">
        <div class="event-club-tag">
          ${event.clubIcon} ${event.clubName}
        </div>
        <div class="event-title">${event.title}</div>
        <div class="event-desc">${event.description || ''}</div>
        <div class="event-meta">
          ${event.eventDate ? `
          <div class="event-meta-row">
            <span class="icon">📅</span>
            <span>${formatDate(event.eventDate)}${event.eventTime ? ' &bull; ' + event.eventTime : ''}</span>
            <span class="event-status-badge ${statusClass}">${event.status}</span>
          </div>` : ''}
          ${event.venue ? `
          <div class="event-meta-row">
            <span class="icon">📍</span>
            <span>${event.venue}</span>
          </div>` : ''}
        </div>
      </div>
    </div>
  `;
}

// ── Load helpers (used by index.html) ─────────────────────
async function loadClubs(gridId, limit) {
  const grid = document.getElementById(gridId);
  if (!grid) return;
  const clubs = await fetchClubs();
  const subset = limit ? clubs.slice(0, limit) : clubs;
  grid.innerHTML = subset.map(clubCardHTML).join('');
}

async function loadUpcomingEvents(gridId) {
  const grid = document.getElementById(gridId);
  if (!grid) return;
  const events = await fetchEvents('?upcoming=true');
  if (!events.length) {
    grid.innerHTML = '<div class="empty-state" style="grid-column:1/-1;"><div class="empty-icon">📅</div><p>No upcoming events at the moment.</p></div>';
    return;
  }
  grid.innerHTML = events.map(eventCardHTML).join('');
}

async function loadEventCount() {
  const el = document.getElementById('totalEvents');
  if (!el) return;
  const events = await fetchEvents();
  el.textContent = events.length;
}
