document.addEventListener('DOMContentLoaded', () => {
  // 1. Services Data
  const services = [
    // Core Infrastructure
    { name: 'Portainer', category: 'infrastructure', catName: 'Core Infrastructure', icon: 'https://cdn.jsdelivr.net/gh/selfhst/icons/png/portainer.png' },
    { name: 'Proxmox VE', category: 'infrastructure', catName: 'Core Infrastructure', icon: 'https://cdn.jsdelivr.net/gh/selfhst/icons/png/proxmox.png' },
    { name: 'TrueNAS Scale / Core', category: 'infrastructure', catName: 'Core Infrastructure', icon: 'https://cdn.jsdelivr.net/gh/selfhst/icons/png/truenas-scale.png' },
    { name: 'Uptime Kuma', category: 'infrastructure', catName: 'Core Infrastructure', icon: 'https://cdn.jsdelivr.net/gh/selfhst/icons/png/uptime-kuma.png' },
    { name: 'Dockhand', category: 'infrastructure', catName: 'Core Infrastructure', icon: 'https://cdn.jsdelivr.net/gh/selfhst/icons/png/dockhand.png' },
    { name: 'DockMon', category: 'infrastructure', catName: 'Core Infrastructure', icon: 'https://cdn.jsdelivr.net/gh/selfhst/icons/png/dockmon.png' },
    { name: 'Komodo', category: 'infrastructure', catName: 'Core Infrastructure', icon: 'assets/icons/komodo.png' },
    { name: 'Beszel', category: 'infrastructure', catName: 'Core Infrastructure', icon: 'https://cdn.jsdelivr.net/gh/selfhst/icons/png/beszel.png' },
    { name: 'Linux Update', category: 'infrastructure', catName: 'Core Infrastructure', icon: 'https://cdn.jsdelivr.net/gh/selfhst/icons/png/linux-update-dashboard.png' },
    { name: 'Crafty Controller', category: 'infrastructure', catName: 'Core Infrastructure', icon: 'https://cdn.jsdelivr.net/gh/selfhst/icons/png/crafty-controller.png' },
    { name: 'Pterodactyl', category: 'infrastructure', catName: 'Core Infrastructure', icon: 'https://cdn.jsdelivr.net/gh/selfhst/icons/png/pterodactyl.png' },
    { name: 'Calagopus', category: 'infrastructure', catName: 'Core Infrastructure', icon: 'https://cdn.jsdelivr.net/gh/selfhst/icons/png/calagopus.png' },
    { name: 'Gitea / Forgejo', category: 'infrastructure', catName: 'Core Infrastructure', icon: 'https://cdn.jsdelivr.net/gh/selfhst/icons/png/gitea.png' },
    { name: 'Pangolin / Newt', category: 'infrastructure', catName: 'Core Infrastructure', icon: 'https://cdn.jsdelivr.net/gh/selfhst/icons/png/pangolin.png' },
    { name: 'Healthchecks', category: 'infrastructure', catName: 'Core Infrastructure', icon: 'https://cdn.jsdelivr.net/gh/selfhst/icons/png/healthchecks.png' },
    { name: 'PatchMon', category: 'infrastructure', catName: 'Core Infrastructure', icon: 'https://cdn.jsdelivr.net/gh/selfhst/icons/png/patchmon.png' },
    { name: 'Wakapi', category: 'infrastructure', catName: 'Core Infrastructure', icon: 'https://cdn.jsdelivr.net/gh/selfhst/icons/png/wakapi.png' },
    
    // Networking & DNS
    { name: 'Pi-hole', category: 'networking', catName: 'Networking & DNS', icon: 'https://cdn.jsdelivr.net/gh/selfhst/icons/png/pi-hole.png' },
    { name: 'AdGuard Home', category: 'networking', catName: 'Networking & DNS', icon: 'https://cdn.jsdelivr.net/gh/selfhst/icons/png/adguard-home.png' },
    { name: 'Ubiquiti Network', category: 'networking', catName: 'Networking & DNS', icon: 'https://cdn.jsdelivr.net/gh/selfhst/icons/png/ubiquiti-unifi.png' },
    { name: 'Technitium DNS', category: 'networking', catName: 'Networking & DNS', icon: 'https://cdn.jsdelivr.net/gh/selfhst/icons/png/technitium.png' },
    { name: 'Maltrail', category: 'networking', catName: 'Networking & DNS', icon: 'assets/icons/maltrail.png' },
    { name: 'Nginx Proxy Manager / NPMplus', category: 'networking', catName: 'Networking & DNS', icon: 'https://cdn.jsdelivr.net/gh/selfhst/icons/png/nginx-proxy-manager.png' },
    
    // Media & Observability
    { name: 'Plex Media Server', category: 'media', catName: 'Media & Observability', icon: 'https://cdn.jsdelivr.net/gh/selfhst/icons/png/plex.png' },
    { name: 'Jellystat', category: 'media', catName: 'Media & Observability', icon: 'https://cdn.jsdelivr.net/gh/selfhst/icons/png/jellystat.png' },
    
    // Servarr Stack
    { name: 'Sonarr', category: 'servarr', catName: 'Servarr Media Stack', icon: 'https://cdn.jsdelivr.net/gh/selfhst/icons/png/sonarr.png' },
    { name: 'Radarr', category: 'servarr', catName: 'Servarr Media Stack', icon: 'https://cdn.jsdelivr.net/gh/selfhst/icons/png/radarr.png' },
    { name: 'Lidarr', category: 'servarr', catName: 'Servarr Media Stack', icon: 'https://cdn.jsdelivr.net/gh/selfhst/icons/png/lidarr.png' },
    { name: 'Prowlarr', category: 'servarr', catName: 'Servarr Media Stack', icon: 'https://cdn.jsdelivr.net/gh/selfhst/icons/png/prowlarr.png' },
    { name: 'qBittorrent', category: 'servarr', catName: 'Servarr Media Stack', icon: 'https://cdn.jsdelivr.net/gh/selfhst/icons/png/qbittorrent.png' },
    { name: 'Jellyseerr', category: 'servarr', catName: 'Servarr Media Stack', icon: 'https://cdn.jsdelivr.net/gh/selfhst/icons/png/jellyseerr.png' },
    { name: 'Bazarr', category: 'servarr', catName: 'Servarr Media Stack', icon: 'https://cdn.jsdelivr.net/gh/selfhst/icons/png/bazarr.png' },
    { name: 'Gluetun', category: 'servarr', catName: 'Servarr Media Stack', icon: 'https://cdn.jsdelivr.net/gh/selfhst/icons/png/gluetun.png' },
    { name: 'FlareSolverr', category: 'servarr', catName: 'Servarr Media Stack', icon: 'https://cdn.jsdelivr.net/gh/selfhst/icons/png/flaresolverr.png' }
  ];

  // 2. Render & Filter Services
  const servicesGrid = document.getElementById('servicesGrid');
  const serviceSearchInput = document.getElementById('serviceSearch');
  const filterButtons = document.querySelectorAll('.service-filter-btn');

  let activeFilter = 'all';
  let searchQuery = '';

  function renderServices() {
    if (!servicesGrid) return;
    
    const filtered = services.filter(service => {
      const matchesFilter = activeFilter === 'all' || service.category === activeFilter;
      const matchesSearch = service.name.toLowerCase().includes(searchQuery.toLowerCase()) || 
                            service.catName.toLowerCase().includes(searchQuery.toLowerCase());
      return matchesFilter && matchesSearch;
    });

    if (filtered.length === 0) {
      servicesGrid.innerHTML = `
        <div style="grid-column: 1 / -1; text-align: center; padding: 36px 16px; color: var(--text-muted); font-size: 0.95rem;">
          No service found matching "<strong>${escapeHtml(searchQuery)}</strong>"
        </div>
      `;
      return;
    }

    servicesGrid.innerHTML = filtered.map(s => `
      <div class="service-card">
        <div class="service-card-icon">
          <img src="${s.icon}" alt="${s.name}" loading="lazy" onerror="this.src='https://cdn.jsdelivr.net/gh/selfhst/icons/png/docker.png'" />
        </div>
        <div class="service-card-text">
          <div class="service-card-name" title="${s.name}">${s.name}</div>
          <div class="service-card-category">${s.catName}</div>
        </div>
      </div>
    `).join('');
  }

  function escapeHtml(str) {
    return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#039;");
  }

  if (serviceSearchInput) {
    serviceSearchInput.addEventListener('input', (e) => {
      searchQuery = e.target.value.trim();
      renderServices();
    });
  }

  filterButtons.forEach(btn => {
    btn.addEventListener('click', () => {
      filterButtons.forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      activeFilter = btn.dataset.filter || 'all';
      renderServices();
    });
  });

  renderServices();

  // 3. Screenshot Showcase Tabs
  const showcaseTabs = document.querySelectorAll('.showcase-tab');
  const showcaseImg = document.getElementById('showcaseImg');
  const showcaseTitle = document.getElementById('showcaseTitle');
  const showcaseDesc = document.getElementById('showcaseDesc');
  const showcaseList = document.getElementById('showcaseList');

  const showcaseData = {
    dashboard: {
      img: 'assets/screenshots/Dashboard.jpg',
      title: 'Unified Homelab Overview',
      desc: 'Get an instant high-level view of all your self-hosted services, server statuses, resource metrics, and quick action shortcuts in one single screen.',
      points: [
        'Instant online/offline health status across nodes',
        'Customizable service cards with live counters',
        'Quick navigation to specialized sub-dashboards'
      ]
    },
    servarr: {
      img: 'assets/screenshots/Servarr.jpg',
      title: 'Unified Servarr Automation',
      desc: 'Control your entire media automation workflow without switching between multiple web portals.',
      points: [
        'Sonarr, Radarr, Lidarr, Prowlarr in one view',
        'qBittorrent active downloads, upload rates & limits',
        'Gluetun VPN tunnel status and IP verification'
      ]
    },
    containers: {
      img: 'assets/screenshots/photo_1_2026-03-16_20-24-21.jpg',
      title: 'Portainer & Container Fleet',
      desc: 'Inspect running containers, stacks, images, and endpoints across local and remote Docker nodes with one-tap power controls.',
      points: [
        'Start, stop, and restart containers instantly',
        'Real-time CPU and Memory resource graphs',
        'Multiple Portainer and DockMon endpoint switching'
      ]
    },
    network: {
      img: 'assets/screenshots/photo_16_2026-03-16_20-24-21.jpg',
      title: 'DNS & Reverse Proxy Armor',
      desc: 'Monitor query logs, blocked trackers, and active reverse proxy streams directly from your Android phone.',
      points: [
        'Pi-hole and AdGuard Home toggle controls with timers',
        'Top blocked domains and client statistics',
        'Nginx Proxy Manager certificate and host management'
      ]
    },
    monitoring: {
      img: 'assets/screenshots/photo_4_2026-03-16_20-24-21.jpg',
      title: 'Beszel & Node Observability',
      desc: 'Track system metrics, disk usage, CPU temperature, and network throughput across bare-metal and VM instances.',
      points: [
        'Multi-host agent telemetry in a single clean graph',
        'Disk capacity warnings and alerts',
        'Lightweight, battery-friendly polling'
      ]
    }
  };

  showcaseTabs.forEach(tab => {
    tab.addEventListener('click', () => {
      showcaseTabs.forEach(t => t.classList.remove('active'));
      tab.classList.add('active');

      const key = tab.dataset.showcase;
      const data = showcaseData[key];
      if (data && showcaseImg) {
        showcaseImg.style.opacity = '0';
        setTimeout(() => {
          showcaseImg.src = data.img;
          showcaseTitle.textContent = data.title;
          showcaseDesc.textContent = data.desc;
          showcaseList.innerHTML = data.points.map(p => `
            <li>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
              <span>${p}</span>
            </li>
          `).join('');
          showcaseImg.style.opacity = '1';
        }, 150);
      }
    });
  });

  // 4. Code Copy Button
  const copyBtn = document.getElementById('copyBuildCmd');
  if (copyBtn) {
    copyBtn.addEventListener('click', () => {
      const code = './gradlew :app:assembleRelease';
      navigator.clipboard.writeText(code).then(() => {
        copyBtn.textContent = 'Copied!';
        setTimeout(() => {
          copyBtn.textContent = 'Copy';
        }, 2000);
      });
    });
  }

  // 5. Privacy Modal
  const openPrivacyBtn = document.getElementById('openPrivacyModal');
  const closePrivacyBtn = document.getElementById('closePrivacyModal');
  const privacyModal = document.getElementById('privacyModal');

  if (openPrivacyBtn && privacyModal) {
    openPrivacyBtn.addEventListener('click', (e) => {
      e.preventDefault();
      privacyModal.classList.add('active');
      document.body.style.overflow = 'hidden';
    });
  }

  if (closePrivacyBtn && privacyModal) {
    closePrivacyBtn.addEventListener('click', () => {
      privacyModal.classList.remove('active');
      document.body.style.overflow = '';
    });

    privacyModal.addEventListener('click', (e) => {
      if (e.target === privacyModal) {
        privacyModal.classList.remove('active');
        document.body.style.overflow = '';
      }
    });
  }

  // 6. Mobile Hamburger Navigation
  const navToggleBtn = document.getElementById('navToggleBtn');
  const navMobileMenu = document.getElementById('navMobileMenu');

  if (navToggleBtn && navMobileMenu) {
    navToggleBtn.addEventListener('click', () => {
      navMobileMenu.classList.toggle('active');
    });

    // Close menu when clicking any mobile nav link
    const mobileLinks = navMobileMenu.querySelectorAll('a');
    mobileLinks.forEach(link => {
      link.addEventListener('click', () => {
        navMobileMenu.classList.remove('active');
      });
    });
  }

  // 7. Dynamic GitHub Release APK Link Resolution
  const heroDownloadBtn = document.getElementById('heroDownloadBtn');
  const heroDownloadText = document.getElementById('heroDownloadText');
  const directDownloadButtons = document.querySelectorAll('.direct-download-btn');

  // Fetch latest release metadata from GitHub API
  fetch('https://api.github.com/repos/AnARCHIS12/homelab/releases/latest')
    .then(res => {
      if (res.ok) return res.json();
      throw new Error('No release found');
    })
    .then(data => {
      if (data && data.tag_name) {
        const tag = data.tag_name;
        // Check for APK asset
        const apkAsset = data.assets && data.assets.find(a => a.name.endsWith('.apk'));
        const downloadUrl = apkAsset ? apkAsset.browser_download_url : `https://github.com/AnARCHIS12/homelab/releases/download/${tag}/Homelab.apk`;
        
        let sizeText = '';
        if (apkAsset && apkAsset.size) {
          const mb = (apkAsset.size / (1024 * 1024)).toFixed(1);
          sizeText = ` · ${mb} MB`;
        }

        if (heroDownloadText) {
          heroDownloadText.textContent = `Download APK (${tag}${sizeText})`;
        }

        directDownloadButtons.forEach(btn => {
          btn.href = downloadUrl;
          btn.setAttribute('download', 'Homelab.apk');
        });
      }
    })
    .catch(() => {
      // Graceful fallback to latest download redirect
      const defaultUrl = 'https://github.com/AnARCHIS12/homelab/releases/latest/download/Homelab.apk';
      directDownloadButtons.forEach(btn => {
        btn.href = defaultUrl;
      });
    });
});
