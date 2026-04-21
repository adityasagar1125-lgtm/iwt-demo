const CTX = (() => {
  const parts = window.location.pathname.split('/').filter(Boolean);
  return parts.length ? `/${parts[0]}` : '';
})();

function apiUrl(path) {
  return `${CTX}${path}`;
}

function getSessionUser() {
  try {
    const raw = sessionStorage.getItem('ccmsUser');
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

function setSessionUser(user) {
  sessionStorage.setItem('ccmsUser', JSON.stringify(user));
}

function clearSessionUser() {
  sessionStorage.removeItem('ccmsUser');
}

function requireAuth(roles = []) {
  const user = getSessionUser();
  if (!user) {
    window.location.href = 'login.html';
    return null;
  }
  if (roles.length && !roles.includes(user.role)) {
    window.location.href = 'dashboard.html';
    return null;
  }
  return user;
}

async function request(path, options = {}) {
  const res = await fetch(apiUrl(path), {
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
    ...options
  });
  let data = {};
  try {
    data = await res.json();
  } catch {
    data = {};
  }
  if (!res.ok) {
    throw new Error(data.message || data.error || `Request failed (${res.status})`);
  }
  return data;
}

async function fetchClubs() {
  const data = await request('/api/clubs');
  return data.clubs || data;
}

async function fetchClubDetail(clubId, userId) {
  const q = userId ? `?id=${clubId}&userId=${userId}` : `?id=${clubId}`;
  return request(`/api/clubs${q}`);
}

async function joinClub(userId, clubId, action = 'join') {
  return request('/api/clubs/join', {
    method: 'POST',
    body: JSON.stringify({ userId, clubId, action })
  });
}

async function fetchEvents(params = '') {
  const data = await request(`/api/events${params}`);
  return data.events || data;
}

async function fetchEventById(eventId) {
  const data = await request(`/api/events?id=${eventId}`);
  return data.event;
}

async function registerEvent(userId, eventId) {
  return request('/api/events/register', {
    method: 'POST',
    body: JSON.stringify({ userId, eventId })
  });
}

async function fetchAnnouncements(clubId) {
  const q = clubId ? `?clubId=${clubId}` : '';
  const data = await request(`/api/announcements${q}`);
  return data.items || [];
}

async function postAnnouncement(payload) {
  return request('/api/announcements', {
    method: 'POST',
    body: JSON.stringify(payload)
  });
}

async function registerUser(payload) {
  return request('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(payload)
  });
}

async function loginUser(payload) {
  return request('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(payload)
  });
}

async function getProfile(userId) {
  return request(`/api/auth/profile?userId=${userId}`);
}

async function updateProfile(payload) {
  return request('/api/auth/profile', {
    method: 'PUT',
    body: JSON.stringify(payload)
  });
}

async function changePassword(payload) {
  return request('/api/auth/change-password', {
    method: 'POST',
    body: JSON.stringify(payload)
  });
}

async function fetchAdminDashboard(userId) {
  return request(`/api/admin/dashboard?userId=${userId}`);
}

async function fetchPendingClubs(userId) {
  const data = await request(`/api/admin/clubs/pending?userId=${userId}`);
  return data.items || [];
}

async function reviewPendingClub(userId, clubId, action) {
  return request('/api/admin/clubs/status', {
    method: 'POST',
    body: JSON.stringify({ userId, clubId, action })
  });
}

function initials(text) {
  if (!text) return 'CL';
  const words = text.trim().split(/\s+/);
  return words.slice(0, 2).map(w => w[0].toUpperCase()).join('');
}

function clubCardHTML(club, user) {
  const mark = initials(club.shortName || club.name);
  const canJoin = Boolean(user);
  return `
    <article class="club-card" style="--accent:${club.colorAccent || '#2c6bff'};">
      <div class="club-card-top">
        <div class="club-icon">${mark}</div>
        <span class="club-category-tag">${club.category || 'General'}</span>
      </div>
      <div class="club-card-name">${club.name}</div>
      <div class="club-card-desc">${club.description || ''}</div>
      <div class="club-card-footer">
        <span class="club-members">${club.memberCount || 0} members</span>
        <div class="inline-actions">
          <a class="action-btn" href="club-detail.html?id=${club.id}">Details</a>
          ${canJoin ? `<button class="action-btn" data-action="join" data-club-id="${club.id}">Join</button>` : ''}
        </div>
      </div>
    </article>
  `;
}

function eventCardHTML(event, user) {
  const showRegister = Boolean(user);
  const remaining = event.remainingSeats != null ? event.remainingSeats : Math.max(0, (event.maxSeats || 100) - (event.registeredCount || 0));
  return `
    <article class="event-card">
      <div class="event-card-body">
        <div class="event-club-tag">${(event.clubName || 'Club').toUpperCase()}</div>
        <h3 class="event-title">${event.title}</h3>
        <p class="event-desc">${event.description || ''}</p>
        <div class="event-meta">
          <div class="event-meta-row"><span>Date:</span> <span>${event.eventDate || ''} ${event.eventTime || ''}</span></div>
          <div class="event-meta-row"><span>Venue:</span> <span>${event.venue || ''}</span></div>
          <div class="event-meta-row"><span>Seats Left:</span> <span>${remaining}</span></div>
        </div>
        <div class="inline-actions" style="margin-top:12px;">
          <a class="action-btn" href="event-detail.html?id=${event.id}">Details</a>
          ${showRegister ? `<button class="action-btn" data-action="register-event" data-event-id="${event.id}">Register</button>` : ''}
        </div>
      </div>
    </article>
  `;
}

function announcementHTML(item) {
  const priority = item.priority || 'normal';
  return `
    <article class="feed-item priority-${priority}">
      <strong>${item.title}</strong>
      <p class="muted" style="margin:6px 0 0;">${item.body}</p>
      <small class="muted">${item.clubName || ''} ${item.createdAt ? ' • ' + item.createdAt : ''}</small>
    </article>
  `;
}

function setMessage(id, text, ok = false) {
  const el = document.getElementById(id);
  if (!el) return;
  el.textContent = text;
  el.style.color = ok ? '#6fe3a6' : '#ff9c9c';
}

function bindLogout() {
  document.querySelectorAll('[data-action="logout"]').forEach(btn => {
    btn.addEventListener('click', () => {
      clearSessionUser();
      window.location.href = 'login.html';
    });
  });
}

async function initIndex() {
  const user = getSessionUser();
  const clubs = await fetchClubs();
  const events = await fetchEvents('?upcoming=true');

  const clubsGrid = document.getElementById('clubsGrid');
  const eventsGrid = document.getElementById('eventsGrid');
  const count = document.getElementById('totalEvents');

  if (clubsGrid) clubsGrid.innerHTML = clubs.slice(0, 6).map(c => clubCardHTML(c, user)).join('');
  if (eventsGrid) eventsGrid.innerHTML = events.slice(0, 6).map(e => eventCardHTML(e, user)).join('');
  if (count) count.textContent = String(events.length);

  clubsGrid?.addEventListener('click', async (e) => {
    const btn = e.target.closest('[data-action="join"]');
    if (!btn) return;
    if (!user) {
      window.location.href = 'login.html';
      return;
    }
    await joinClub(user.userId, Number(btn.dataset.clubId));
    setMessage('pageMessage', 'Joined successfully.', true);
  });
}

async function initLogin() {
  const form = document.getElementById('loginForm');
  form?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const fd = new FormData(form);
    try {
      const out = await loginUser({ email: fd.get('email'), password: fd.get('password') });
      setSessionUser(out);
      window.location.href = 'dashboard.html';
    } catch (err) {
      setMessage('authMessage', err.message);
    }
  });
}

async function initRegister() {
  const form = document.getElementById('registerForm');
  form?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const fd = new FormData(form);
    try {
      const out = await registerUser({
        name: fd.get('name'),
        email: fd.get('email'),
        rollNumber: fd.get('rollNumber'),
        password: fd.get('password')
      });
      setSessionUser({ userId: out.userId, name: fd.get('name'), email: fd.get('email'), role: out.role });
      window.location.href = 'dashboard.html';
    } catch (err) {
      setMessage('authMessage', err.message);
    }
  });
}

async function initDashboard() {
  const user = requireAuth();
  if (!user) return;
  bindLogout();

  const [clubs, events, feed] = await Promise.all([fetchClubs(), fetchEvents(), fetchAnnouncements()]);

  document.getElementById('welcomeName').textContent = user.name;
  document.getElementById('kpiClubs').textContent = String(clubs.length);
  document.getElementById('kpiEvents').textContent = String(events.length);
  document.getElementById('kpiAnnouncements').textContent = String(feed.length);

  document.getElementById('announcementFeed').innerHTML = feed.slice(0, 8).map(announcementHTML).join('') || '<p class="muted">No announcements available.</p>';
}

async function initClubsPage() {
  const user = getSessionUser();
  const clubs = await fetchClubs();
  const grid = document.getElementById('clubsGrid');
  const search = document.getElementById('searchClub');
  const category = document.getElementById('categoryFilter');

  const cats = ['all', ...new Set(clubs.map(c => c.category).filter(Boolean))];
  category.innerHTML = cats.map(c => `<option value="${c}">${c === 'all' ? 'All categories' : c}</option>`).join('');

  function render() {
    const q = (search.value || '').toLowerCase().trim();
    const cat = category.value;
    const filtered = clubs.filter(c => {
      const matchQ = !q || `${c.name} ${c.description} ${c.shortName}`.toLowerCase().includes(q);
      const matchC = cat === 'all' || c.category === cat;
      return matchQ && matchC;
    });
    grid.innerHTML = filtered.map(c => clubCardHTML(c, user)).join('');
  }

  search.addEventListener('input', render);
  category.addEventListener('change', render);
  grid.addEventListener('click', async (e) => {
    const btn = e.target.closest('[data-action="join"]');
    if (!btn) return;
    if (!user) {
      window.location.href = 'login.html';
      return;
    }
    try {
      await joinClub(user.userId, Number(btn.dataset.clubId));
      setMessage('pageMessage', 'Joined successfully.', true);
    } catch (err) {
      setMessage('pageMessage', err.message);
    }
  });

  render();
}

async function initClubDetail() {
  const user = getSessionUser();
  const params = new URLSearchParams(window.location.search);
  const clubId = params.get('id');
  if (!clubId) {
    window.location.href = 'clubs.html';
    return;
  }

  const data = await fetchClubDetail(clubId, user?.userId);
  const club = data.club;
  document.title = `${club.name} - CCMS`;

  document.getElementById('clubHero').innerHTML = `
    <div class="club-hero-icon">${club.shortName || initials(club.name)}</div>
    <div class="club-hero-info">
      <h1>${club.name}</h1>
      <p>${club.description || ''}</p>
      <div class="club-meta-chips">
        <span class="chip">Category: ${club.category || 'General'}</span>
        <span class="chip">Members: ${club.memberCount || 0}</span>
      </div>
      ${user ? `<div style="margin-top:12px;"><button id="joinLeaveBtn" class="action-btn">${data.isMember ? 'Leave Club' : 'Join Club'}</button></div>` : ''}
    </div>
  `;

  document.getElementById('clubMembers').innerHTML = (data.members || []).map(m => `<li>${m}</li>`).join('') || '<li>No members yet.</li>';
  document.getElementById('clubAnnouncements').innerHTML = (data.announcements || []).map(announcementHTML).join('') || '<p class="muted">No announcements.</p>';

  const events = await fetchEvents(`?clubId=${clubId}`);
  document.getElementById('eventsGrid').innerHTML = events.map(e => eventCardHTML(e, user)).join('') || '<p class="muted">No events listed.</p>';

  const joinLeaveBtn = document.getElementById('joinLeaveBtn');
  joinLeaveBtn?.addEventListener('click', async () => {
    const action = joinLeaveBtn.textContent.includes('Leave') ? 'leave' : 'join';
    try {
      await joinClub(user.userId, Number(clubId), action);
      joinLeaveBtn.textContent = action === 'join' ? 'Leave Club' : 'Join Club';
      setMessage('pageMessage', `Club ${action === 'join' ? 'joined' : 'left'} successfully.`, true);
    } catch (err) {
      setMessage('pageMessage', err.message);
    }
  });
}

async function initEventsPage() {
  const user = getSessionUser();
  const events = await fetchEvents();
  const grid = document.getElementById('eventsGrid');
  const status = document.getElementById('statusFilter');

  function render() {
    const selected = status.value;
    const filtered = selected === 'all' ? events : events.filter(e => e.status === selected);
    grid.innerHTML = filtered.map(e => eventCardHTML(e, user)).join('');
    document.getElementById('resultsCount').textContent = `Showing ${filtered.length} events`;
  }

  status?.addEventListener('change', render);
  grid.addEventListener('click', async (e) => {
    const btn = e.target.closest('[data-action="register-event"]');
    if (!btn) return;
    if (!user) {
      window.location.href = 'login.html';
      return;
    }
    try {
      const out = await registerEvent(user.userId, Number(btn.dataset.eventId));
      setMessage('pageMessage', `Registration confirmed. Total registrations: ${out.seats}`, true);
    } catch (err) {
      setMessage('pageMessage', err.message);
    }
  });

  render();
}

async function initAdminPage() {
  const user = requireAuth(['super_admin', 'club_admin']);
  if (!user) return;
  bindLogout();

  const stats = await fetchAdminDashboard(user.userId);
  document.getElementById('adminClubs').textContent = stats.clubs;
  document.getElementById('adminMembers').textContent = stats.members;
  document.getElementById('adminEvents').textContent = stats.events;
  document.getElementById('adminUsers').textContent = stats.users;

  const clubs = await fetchClubs();
  document.getElementById('adminClubList').innerHTML = clubs.map(c => `
    <tr>
      <td>${c.name}</td>
      <td>${c.category || ''}</td>
      <td>${c.memberCount || 0}</td>
      <td>${c.status || 'active'}</td>
    </tr>
  `).join('');

  const announceForm = document.getElementById('announcementForm');
  const clubSelect = document.getElementById('clubSelect');
  clubSelect.innerHTML = clubs.map(c => `<option value="${c.id}">${c.name}</option>`).join('');

  if (user.role === 'super_admin') {
    const pending = await fetchPendingClubs(user.userId);
    const table = document.getElementById('pendingClubList');
    const section = document.getElementById('pendingSection');
    if (section) section.style.display = '';
    if (table) {
      table.innerHTML = pending.map(c => `
        <tr>
          <td>${c.name}</td>
          <td>${c.category || ''}</td>
          <td>${c.memberCount || 0}</td>
          <td>
            <div class="inline-actions">
              <button class="action-btn" data-action="approve-club" data-club-id="${c.id}">Approve</button>
              <button class="action-btn" data-action="reject-club" data-club-id="${c.id}">Reject</button>
            </div>
          </td>
        </tr>
      `).join('') || '<tr><td colspan="4" class="muted">No pending clubs.</td></tr>';
    }

    table?.addEventListener('click', async (e) => {
      const approve = e.target.closest('[data-action="approve-club"]');
      const reject = e.target.closest('[data-action="reject-club"]');
      const btn = approve || reject;
      if (!btn) return;
      const action = approve ? 'approve' : 'reject';
      try {
        await reviewPendingClub(user.userId, Number(btn.dataset.clubId), action);
        setMessage('adminMessage', `Club ${action}d successfully.`, true);
        initAdminPage();
      } catch (err) {
        setMessage('adminMessage', err.message);
      }
    });
  }

  announceForm?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const fd = new FormData(announceForm);
    try {
      await postAnnouncement({
        clubId: Number(fd.get('clubId')),
        title: fd.get('title'),
        body: fd.get('body'),
        priority: fd.get('priority')
      });
      announceForm.reset();
      setMessage('adminMessage', 'Announcement posted.', true);
    } catch (err) {
      setMessage('adminMessage', err.message);
    }
  });
}

async function initProfilePage() {
  const user = requireAuth();
  if (!user) return;
  bindLogout();

  const profile = await getProfile(user.userId);
  document.getElementById('profileName').value = profile.name || '';
  document.getElementById('profileBio').value = profile.bio || '';
  document.getElementById('profilePicture').value = profile.profilePicture || '';
  document.getElementById('profileEmail').textContent = profile.email || '';
  document.getElementById('profileRole').textContent = profile.role || '';

  const form = document.getElementById('profileForm');
  form?.addEventListener('submit', async (e) => {
    e.preventDefault();
    try {
      const updated = await updateProfile({
        userId: user.userId,
        name: document.getElementById('profileName').value,
        bio: document.getElementById('profileBio').value,
        profilePicture: document.getElementById('profilePicture').value
      });
      setSessionUser({ ...user, name: updated.name });
      setMessage('profileMessage', 'Profile updated.', true);
    } catch (err) {
      setMessage('profileMessage', err.message);
    }
  });

  const passwordForm = document.getElementById('passwordForm');
  passwordForm?.addEventListener('submit', async (e) => {
    e.preventDefault();
    try {
      await changePassword({
        userId: user.userId,
        currentPassword: document.getElementById('currentPassword').value,
        newPassword: document.getElementById('newPassword').value
      });
      passwordForm.reset();
      setMessage('passwordMessage', 'Password changed successfully.', true);
    } catch (err) {
      setMessage('passwordMessage', err.message);
    }
  });
}

async function initEventDetail() {
  const user = getSessionUser();
  const params = new URLSearchParams(window.location.search);
  const eventId = params.get('id');
  if (!eventId) {
    window.location.href = 'events.html';
    return;
  }

  const event = await fetchEventById(eventId);
  const remaining = event.remainingSeats != null ? event.remainingSeats : Math.max(0, (event.maxSeats || 100) - (event.registeredCount || 0));
  document.getElementById('eventDetail').innerHTML = `
    <div class="panel">
      <h1>${event.title}</h1>
      <p class="muted">${event.clubName || 'Club'} | ${event.eventDate || ''} ${event.eventTime || ''}</p>
      <p>${event.description || ''}</p>
      <div class="grid-2" style="margin-top:12px;">
        <div class="kpi"><div class="muted">Venue</div><div class="kpi-value">${event.venue || '-'}</div></div>
        <div class="kpi"><div class="muted">Seats Left</div><div class="kpi-value">${remaining}</div></div>
      </div>
      <div class="kpi" style="margin-top:12px;"><div class="muted">Starts In</div><div id="countdown" class="kpi-value">Calculating...</div></div>
      ${user ? `<div style="margin-top:14px;"><button class="btn btn-primary" id="registerNowBtn">Register for Event</button></div>` : '<p class="muted" style="margin-top:14px;">Login to register for this event.</p>'}
    </div>
  `;

  let eventDateTime = new Date(`${event.eventDate}T09:00:00`);
  if (event.eventDate && event.eventTime) {
    const parsed = new Date(`${event.eventDate} ${event.eventTime}`);
    if (!Number.isNaN(parsed.getTime())) {
      eventDateTime = parsed;
    }
  }
  const countdownEl = document.getElementById('countdown');
  const timer = setInterval(() => {
    const now = new Date();
    const diff = eventDateTime - now;
    if (Number.isNaN(diff) || Number.isNaN(eventDateTime.getTime())) {
      countdownEl.textContent = 'Date/time unavailable';
      clearInterval(timer);
      return;
    }
    if (diff <= 0) {
      countdownEl.textContent = 'Started';
      clearInterval(timer);
      return;
    }
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));
    const hours = Math.floor((diff / (1000 * 60 * 60)) % 24);
    const mins = Math.floor((diff / (1000 * 60)) % 60);
    countdownEl.textContent = `${days}d ${hours}h ${mins}m`;
  }, 1000);

  document.getElementById('registerNowBtn')?.addEventListener('click', async () => {
    try {
      const out = await registerEvent(user.userId, Number(eventId));
      setMessage('pageMessage', `Registration confirmed. Total registrations: ${out.seats}`, true);
    } catch (err) {
      setMessage('pageMessage', err.message);
    }
  });
}

function updateNavForSession() {
  const user = getSessionUser();
  document.querySelectorAll('[data-auth="only"]').forEach(el => {
    el.style.display = user ? '' : 'none';
  });
  document.querySelectorAll('[data-auth="guest"]').forEach(el => {
    el.style.display = user ? 'none' : '';
  });
  const roleEls = document.querySelectorAll('[data-role]');
  roleEls.forEach(el => {
    const role = el.getAttribute('data-role');
    el.style.display = user && user.role === role ? '' : 'none';
  });
}

document.addEventListener('DOMContentLoaded', async () => {
  updateNavForSession();
  const page = document.body.dataset.page;

  try {
    if (page === 'index') await initIndex();
    if (page === 'login') await initLogin();
    if (page === 'register') await initRegister();
    if (page === 'dashboard') await initDashboard();
    if (page === 'clubs') await initClubsPage();
    if (page === 'club-detail') await initClubDetail();
    if (page === 'events') await initEventsPage();
    if (page === 'event-detail') await initEventDetail();
    if (page === 'admin') await initAdminPage();
    if (page === 'profile') await initProfilePage();
  } catch (err) {
    setMessage('pageMessage', err.message || 'Something went wrong.');
  }
});
