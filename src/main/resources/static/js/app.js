/**
 * NetRoute Web Application Controller
 * Coordinates UI views, user interactions, canvas rendering, and REST API calls.
 */
document.addEventListener('DOMContentLoaded', () => {
    // ---- Global State ----
    let visualizer = null;
    let currentNetwork = null;
    let currentSimulationState = null;
    let currentStatistics = null;
    let activeRoute = null;
    let activeView = 'simulatorView';
    let pollInterval = null;

    // ---- DOM Elements ----
    const elements = {
        // Nav buttons
        brandBtn: document.getElementById('brandBtn'),
        navSimulator: document.getElementById('navSimulator'),
        navTopologies: document.getElementById('navTopologies'),
        navHistory: document.getElementById('navHistory'),
        navTech: document.getElementById('navTech'),
        navAbout: document.getElementById('navAbout'),

        // Hero buttons
        btnHeroLaunch: document.getElementById('btnHeroLaunch'),
        btnHeroTopologies: document.getElementById('btnHeroTopologies'),
        btnHeroHistory: document.getElementById('btnHeroHistory'),

        // Feature cards
        cardFeatRouting: document.getElementById('cardFeatRouting'),
        cardFeatSimulation: document.getElementById('cardFeatSimulation'),
        cardFeatFailures: document.getElementById('cardFeatFailures'),
        cardFeatPersistence: document.getElementById('cardFeatPersistence'),

        // Database status
        dbStatusDot: document.getElementById('dbStatusDot'),
        dbStatusText: document.getElementById('dbStatusText'),

        // Views
        landingView: document.getElementById('landingView'),
        simulatorView: document.getElementById('simulatorView'),
        topologiesView: document.getElementById('topologiesView'),
        historyView: document.getElementById('historyView'),
        techView: document.getElementById('techView'),
        aboutView: document.getElementById('aboutView'),

        // Project header
        lblProjectName: document.getElementById('lblProjectName'),
        badgeModified: document.getElementById('badgeModified'),
        btnNewProject: document.getElementById('btnNewProject'),
        btnSaveProject: document.getElementById('btnSaveProject'),
        btnResetCampusNetwork: document.getElementById('btnResetCampusNetwork'),

        // Canvas controls
        btnZoomIn: document.getElementById('btnZoomIn'),
        btnZoomOut: document.getElementById('btnZoomOut'),
        btnResetZoom: document.getElementById('btnResetZoom'),

        // Routing controls
        selSourceNode: document.getElementById('selSourceNode'),
        selDestNode: document.getElementById('selDestNode'),
        btnCalculateRoute: document.getElementById('btnCalculateRoute'),
        routeResultBox: document.getElementById('routeResultBox'),

        // Failure controls
        selTargetLink: document.getElementById('selTargetLink'),
        btnFailLink: document.getElementById('btnFailLink'),
        btnRestoreLink: document.getElementById('btnRestoreLink'),
        btnRandomizeCosts: document.getElementById('btnRandomizeCosts'),

        // Network management controls
        selAddNodeType: document.getElementById('selAddNodeType'),
        btnAddNode: document.getElementById('btnAddNode'),
        selConnectNodeA: document.getElementById('selConnectNodeA'),
        selConnectNodeB: document.getElementById('selConnectNodeB'),
        txtLinkCost: document.getElementById('txtLinkCost'),
        txtLinkDelay: document.getElementById('txtLinkDelay'),
        txtLinkCapacity: document.getElementById('txtLinkCapacity'),
        btnConnectNodes: document.getElementById('btnConnectNodes'),
        btnDeleteSelectedNode: document.getElementById('btnDeleteSelectedNode'),
        btnDeleteSelectedLink: document.getElementById('btnDeleteSelectedLink'),

        // Simulation controls
        btnSendPacket: document.getElementById('btnSendPacket'),
        btnStepSimulation: document.getElementById('btnStepSimulation'),
        btnRunSimulation: document.getElementById('btnRunSimulation'),
        btnResetSimulation: document.getElementById('btnResetSimulation'),

        // Telemetry metrics
        valTotalPackets: document.getElementById('valTotalPackets'),
        valDelivered: document.getElementById('valDelivered'),
        valFailed: document.getElementById('valFailed'),
        valDropped: document.getElementById('valDropped'),
        valDeliveryRatio: document.getElementById('valDeliveryRatio'),
        valAvgDelay: document.getElementById('valAvgDelay'),
        valTotalNodes: document.getElementById('valTotalNodes'),
        valTotalLinks: document.getElementById('valTotalLinks'),
        valActiveLinks: document.getElementById('valActiveLinks'),
        valInactiveLinks: document.getElementById('valInactiveLinks'),
        valFailureEvents: document.getElementById('valFailureEvents'),
        valReroutes: document.getElementById('valReroutes'),

        // Event log
        terminalBox: document.getElementById('terminalBox'),
        btnClearLog: document.getElementById('btnClearLog'),

        // Topologies view
        txtSearchTopologies: document.getElementById('txtSearchTopologies'),
        btnOpenSaveModal: document.getElementById('btnOpenSaveModal'),
        topologiesGrid: document.getElementById('topologiesGrid'),

        // History view
        txtSearchRuns: document.getElementById('txtSearchRuns'),
        btnRefreshRuns: document.getElementById('btnRefreshRuns'),
        runsTableBody: document.getElementById('runsTableBody'),

        // Modals
        modalSaveTopology: document.getElementById('modalSaveTopology'),
        txtSaveTopoName: document.getElementById('txtSaveTopoName'),
        txtSaveTopoDesc: document.getElementById('txtSaveTopoDesc'),
        chkOverwriteIfExists: document.getElementById('chkOverwriteIfExists'),
        chkRecordSimulationRun: document.getElementById('chkRecordSimulationRun'),
        groupRunNotes: document.getElementById('groupRunNotes'),
        txtSaveRunNotes: document.getElementById('txtSaveRunNotes'),
        btnConfirmSave: document.getElementById('btnConfirmSave'),

        modalRunDetails: document.getElementById('modalRunDetails'),
        runDetailsBody: document.getElementById('runDetailsBody'),
        lblRunModalTitle: document.getElementById('lblRunModalTitle'),

        toastContainer: document.getElementById('toastContainer')
    };

    // ---- Initialize Visualizer ----
    try {
        visualizer = new NetworkVisualizer('networkCanvas');
    } catch (e) {
        console.error('Failed to initialize canvas visualizer:', e);
    }

    // Callback when a node is selected on the canvas
    window.onNodeSelected = (node) => {
        if (!elements.selSourceNode.value) {
            elements.selSourceNode.value = node.id;
        } else if (!elements.selDestNode.value || elements.selDestNode.value === elements.selSourceNode.value) {
            elements.selDestNode.value = node.id;
        } else {
            elements.selSourceNode.value = node.id;
        }
    };

    // ---- View Navigation ----
    const navMap = {
        'landingView': elements.brandBtn,
        'simulatorView': elements.navSimulator,
        'topologiesView': elements.navTopologies,
        'historyView': elements.navHistory,
        'techView': elements.navTech,
        'aboutView': elements.navAbout
    };

    function switchView(viewId) {
        activeView = viewId;
        document.querySelectorAll('.view').forEach(view => {
            view.classList.toggle('active', view.id === viewId);
        });

        document.querySelectorAll('.nav-btn').forEach(btn => btn.classList.remove('active'));
        if (navMap[viewId] && navMap[viewId] !== elements.brandBtn) {
            navMap[viewId].classList.add('active');
        }

        if (viewId === 'simulatorView') {
            if (visualizer) {
                setTimeout(() => visualizer.resize(), 50);
            }
            startPolling();
        } else {
            stopPolling();
            if (viewId === 'topologiesView') {
                loadTopologiesList();
            } else if (viewId === 'historyView') {
                loadSimulationHistoryList();
            }
        }
    }

    elements.brandBtn.addEventListener('click', () => switchView('landingView'));
    elements.navSimulator.addEventListener('click', () => switchView('simulatorView'));
    elements.navTopologies.addEventListener('click', () => switchView('topologiesView'));
    elements.navHistory.addEventListener('click', () => switchView('historyView'));
    elements.navTech.addEventListener('click', () => switchView('techView'));
    elements.navAbout.addEventListener('click', () => switchView('aboutView'));

    elements.btnHeroLaunch.addEventListener('click', () => switchView('simulatorView'));
    elements.btnHeroTopologies.addEventListener('click', () => switchView('topologiesView'));
    elements.btnHeroHistory.addEventListener('click', () => switchView('historyView'));

    elements.cardFeatRouting.addEventListener('click', () => switchView('simulatorView'));
    elements.cardFeatSimulation.addEventListener('click', () => switchView('simulatorView'));
    elements.cardFeatFailures.addEventListener('click', () => switchView('simulatorView'));
    elements.cardFeatPersistence.addEventListener('click', () => switchView('topologiesView'));

    // ---- Canvas Zoom / Reset Handlers ----
    if (elements.btnZoomIn) elements.btnZoomIn.addEventListener('click', () => visualizer && visualizer.zoomIn());
    if (elements.btnZoomOut) elements.btnZoomOut.addEventListener('click', () => visualizer && visualizer.zoomOut());
    if (elements.btnResetZoom) elements.btnResetZoom.addEventListener('click', () => visualizer && visualizer.resetView());

    // ---- Toast Notifications ----
    function showToast(message, type = 'info') {
        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        toast.innerHTML = `<span>${type === 'success' ? '✓' : (type === 'error' ? '✕' : 'ℹ')}</span><span>${escapeHtml(message)}</span>`;
        elements.toastContainer.appendChild(toast);

        setTimeout(() => toast.classList.add('show'), 10);
        setTimeout(() => {
            toast.classList.remove('show');
            setTimeout(() => toast.remove(), 300);
        }, 3500);
    }

    function escapeHtml(str) {
        if (!str) return '';
        return str.replace(/[&<>"']/g, m => ({
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#39;'
        }[m]));
    }

    // ---- Data Syncing ----
    async function syncNetwork() {
        try {
            currentNetwork = await API.getNetwork();
            updateNetworkUI();
        } catch (e) {
            console.error('Error fetching network:', e);
        }
    }

    async function syncSimulationState() {
        try {
            currentSimulationState = await API.getSimulationState();
            updateSimulationUI();
        } catch (e) {
            console.error('Error fetching simulation state:', e);
        }
    }

    async function syncStatistics() {
        try {
            currentStatistics = await API.getStatistics();
            updateStatisticsUI();
        } catch (e) {
            console.error('Error fetching statistics:', e);
        }
    }

    async function checkDatabaseStatus() {
        try {
            const status = await API.getDbStatus();
            if (status.available) {
                elements.dbStatusDot.className = 'status-dot online';
                elements.dbStatusText.textContent = 'MySQL Connected';
                elements.dbStatusBadge.title = `${status.databaseProduct} ${status.databaseVersion} (${status.responseTimeMs}ms)`;
            } else {
                elements.dbStatusDot.className = 'status-dot';
                elements.dbStatusText.textContent = 'MySQL Offline';
                elements.dbStatusBadge.title = status.message;
            }
        } catch (e) {
            elements.dbStatusDot.className = 'status-dot';
            elements.dbStatusText.textContent = 'DB Check Failed';
        }
    }

    function updateNetworkUI() {
        if (!currentNetwork) return;

        // Update Project Title & Badge
        elements.lblProjectName.textContent = currentNetwork.activeTopologyName || 'Untitled Project';
        elements.badgeModified.style.display = currentNetwork.topologyModified ? 'inline-block' : 'none';

        // Populate Node Selects
        const nodes = currentNetwork.nodes || [];
        const prevSrc = elements.selSourceNode.value;
        const prevDst = elements.selDestNode.value;
        const prevConnA = elements.selConnectNodeA.value;
        const prevConnB = elements.selConnectNodeB.value;

        const populateOptions = (selectElem, placeholder = null) => {
            selectElem.innerHTML = '';
            if (placeholder) {
                const opt = document.createElement('option');
                opt.value = '';
                opt.textContent = placeholder;
                selectElem.appendChild(opt);
            }
            nodes.forEach(n => {
                const opt = document.createElement('option');
                opt.value = n.id;
                opt.textContent = `${n.id} (${n.name})`;
                selectElem.appendChild(opt);
            });
        };

        populateOptions(elements.selSourceNode);
        populateOptions(elements.selDestNode);
        populateOptions(elements.selConnectNodeA);
        populateOptions(elements.selConnectNodeB);

        if (nodes.some(n => n.id === prevSrc)) elements.selSourceNode.value = prevSrc;
        else if (nodes.length > 0) elements.selSourceNode.value = nodes[0].id;

        if (nodes.some(n => n.id === prevDst)) elements.selDestNode.value = prevDst;
        else if (nodes.length > 1) elements.selDestNode.value = nodes[nodes.length - 1].id;

        if (nodes.some(n => n.id === prevConnA)) elements.selConnectNodeA.value = prevConnA;
        if (nodes.some(n => n.id === prevConnB)) elements.selConnectNodeB.value = prevConnB;

        // Populate Links Select
        const links = currentNetwork.links || [];
        const prevLink = elements.selTargetLink.value;
        elements.selTargetLink.innerHTML = '';
        links.forEach(l => {
            const opt = document.createElement('option');
            opt.value = l.id;
            opt.textContent = `${l.id}: ${l.sourceId} ⇄ ${l.destinationId} (cost=${l.cost})${!l.active ? ' [FAILED]' : ''}`;
            elements.selTargetLink.appendChild(opt);
        });
        if (links.some(l => l.id === prevLink)) {
            elements.selTargetLink.value = prevLink;
        }

        // Render Canvas
        if (visualizer) {
            visualizer.setData(nodes, links, activeRoute, currentSimulationState ? currentSimulationState.packets : []);
        }
    }

    function updateSimulationUI() {
        if (!currentSimulationState) return;

        // Update Event Log Terminal
        if (currentSimulationState.recentEvents && elements.terminalBox) {
            elements.terminalBox.innerHTML = '';
            currentSimulationState.recentEvents.forEach(line => {
                const div = document.createElement('div');
                div.className = 'terminal-line';
                if (line.includes('FAILED')) div.classList.add('terminal-err');
                else if (line.includes('RESTORED') || line.includes('Calculated shortest')) div.classList.add('terminal-highlight');
                else if (line.includes('tick') || line.includes('Simulated')) div.classList.add('terminal-time');
                div.textContent = line;
                elements.terminalBox.appendChild(div);
            });
            elements.terminalBox.scrollTop = elements.terminalBox.scrollHeight;
        }

        // Pass packets to visualizer
        if (visualizer) {
            visualizer.setPackets(currentSimulationState.packets);
        }
    }

    function updateStatisticsUI() {
        if (!currentStatistics) return;

        elements.valTotalPackets.textContent = currentStatistics.totalPackets;
        elements.valDelivered.textContent = currentStatistics.deliveredCount;
        elements.valFailed.textContent = currentStatistics.failedCount;
        elements.valDropped.textContent = currentStatistics.droppedCount;
        elements.valDeliveryRatio.textContent = `${currentStatistics.deliveryRatio.toFixed(1)}%`;
        elements.valAvgDelay.textContent = `${currentStatistics.averageDeliveryDelay.toFixed(2)}t`;

        elements.valTotalNodes.textContent = currentStatistics.totalNodes;
        elements.valTotalLinks.textContent = currentStatistics.totalLinks;
        elements.valActiveLinks.textContent = currentStatistics.activeLinks;
        elements.valInactiveLinks.textContent = currentStatistics.inactiveLinks;
        elements.valFailureEvents.textContent = currentStatistics.linkFailureCount;
        elements.valReroutes.textContent = currentStatistics.totalReroutes;
    }

    // ---- Polling for Real-Time Sync ----
    function startPolling() {
        if (pollInterval) return;
        pollInterval = setInterval(async () => {
            if (activeView === 'simulatorView') {
                await syncSimulationState();
                await syncStatistics();
            }
        }, 1500);
    }

    function stopPolling() {
        if (pollInterval) {
            clearInterval(pollInterval);
            pollInterval = null;
        }
    }

    // ---- Routing Action ----
    elements.btnCalculateRoute.addEventListener('click', async () => {
        const src = elements.selSourceNode.value;
        const dst = elements.selDestNode.value;

        if (!src || !dst) {
            showToast('Please select source and destination nodes.', 'warn');
            return;
        }

        try {
            const route = await API.calculateRoute(src, dst);
            if (route && route.found) {
                activeRoute = route;
                if (visualizer) visualizer.setRoute(route);
                elements.routeResultBox.className = 'route-result-box active';
                elements.routeResultBox.innerHTML = `
                    <div><strong>Optimal Path:</strong> <span style="color: var(--accent-cyan); font-weight: 600;">${route.nodeIds.join(' → ')}</span></div>
                    <div style="margin-top: 4px;">Total Cost: <span class="route-cost">${route.totalCost}</span> (${route.links.length} hops)</div>
                `;
                showToast(`Shortest route found! Cost: ${route.totalCost}`, 'success');
            } else {
                activeRoute = null;
                if (visualizer) visualizer.setRoute(null);
                elements.routeResultBox.className = 'route-result-box';
                elements.routeResultBox.innerHTML = `<span style="color: var(--status-error);">No reachable route found between ${src} and ${dst}.</span>`;
                showToast(`No available path between ${src} and ${dst}`, 'warn');
            }
            await syncSimulationState();
        } catch (e) {
            showToast(e.message, 'error');
        }
    });

    // ---- Simulation Actions ----
    elements.btnSendPacket.addEventListener('click', async () => {
        const src = elements.selSourceNode.value;
        const dst = elements.selDestNode.value;

        if (!src || !dst) {
            showToast('Please select source and destination nodes.', 'warn');
            return;
        }

        try {
            await API.submitPacket(src, dst, 64);
            showToast(`Packet submitted from ${src} to ${dst}`, 'success');
            await syncSimulationState();
            await syncStatistics();
        } catch (e) {
            showToast(e.message, 'error');
        }
    });

    elements.btnStepSimulation.addEventListener('click', async () => {
        try {
            await API.stepSimulation();
            await syncSimulationState();
            await syncStatistics();
        } catch (e) {
            showToast(e.message, 'error');
        }
    });

    elements.btnRunSimulation.addEventListener('click', async () => {
        try {
            await API.runSimulation();
            showToast('Simulation ran to idle state.', 'info');
            await syncSimulationState();
            await syncStatistics();
        } catch (e) {
            showToast(e.message, 'error');
        }
    });

    elements.btnResetSimulation.addEventListener('click', async () => {
        try {
            await API.resetSimulation();
            showToast('Simulation engine reset.', 'info');
            await syncSimulationState();
            await syncStatistics();
        } catch (e) {
            showToast(e.message, 'error');
        }
    });

    // ---- Link Failure & Restoration ----
    elements.btnFailLink.addEventListener('click', async () => {
        const linkId = elements.selTargetLink.value;
        if (!linkId) {
            showToast('Please select a link to fail.', 'warn');
            return;
        }

        try {
            await API.failLink(linkId);
            showToast(`Link ${linkId} failed. Live packets rerouted or dropped.`, 'warn');
            await syncNetwork();
            await syncSimulationState();
            await syncStatistics();
        } catch (e) {
            showToast(e.message, 'error');
        }
    });

    elements.btnRestoreLink.addEventListener('click', async () => {
        const linkId = elements.selTargetLink.value;
        if (!linkId) {
            showToast('Please select a link to restore.', 'warn');
            return;
        }

        try {
            await API.restoreLink(linkId);
            showToast(`Link ${linkId} restored to active operation.`, 'success');
            await syncNetwork();
            await syncSimulationState();
            await syncStatistics();
        } catch (e) {
            showToast(e.message, 'error');
        }
    });

    elements.btnRandomizeCosts.addEventListener('click', async () => {
        try {
            await API.randomizeCosts();
            showToast('Link costs randomized between 1 and 10.', 'info');
            activeRoute = null;
            if (visualizer) visualizer.setRoute(null);
            elements.routeResultBox.innerHTML = `<span>No route calculated yet.</span>`;
            elements.routeResultBox.className = 'route-result-box';
            await syncNetwork();
            await syncSimulationState();
        } catch (e) {
            showToast(e.message, 'error');
        }
    });

    // ---- Network Management (Add/Connect/Delete) ----
    elements.btnAddNode.addEventListener('click', async () => {
        const type = elements.selAddNodeType.value;
        try {
            const node = await API.addNode(type);
            showToast(`Added ${type} node: ${node.id}`, 'success');
            await syncNetwork();
            await syncStatistics();
        } catch (e) {
            showToast(e.message, 'error');
        }
    });

    elements.btnConnectNodes.addEventListener('click', async () => {
        const src = elements.selConnectNodeA.value;
        const dst = elements.selConnectNodeB.value;
        const cost = parseFloat(elements.txtLinkCost.value);
        const delay = parseFloat(elements.txtLinkDelay.value) || 2.0;
        const cap = parseInt(elements.txtLinkCapacity.value) || 4;

        if (!src || !dst) {
            showToast('Select two distinct nodes to connect.', 'warn');
            return;
        }
        if (src === dst) {
            showToast('Self-loops are not permitted.', 'warn');
            return;
        }
        if (isNaN(cost) || cost <= 0) {
            showToast('Routing cost must be a positive number.', 'warn');
            return;
        }

        try {
            const link = await API.connectNodes(src, dst, cost, delay, cap, false);
            showToast(`Connected ${src} and ${dst} via link ${link.id}`, 'success');
            await syncNetwork();
            await syncStatistics();
        } catch (e) {
            showToast(e.message, 'error');
        }
    });

    elements.btnDeleteSelectedNode.addEventListener('click', async () => {
        const nodeId = elements.selSourceNode.value;
        if (!nodeId) {
            showToast('Select a node in Source Node dropdown to delete.', 'warn');
            return;
        }
        if (!confirm(`Are you sure you want to delete node ${nodeId} and all connected links?`)) {
            return;
        }

        try {
            await API.deleteNode(nodeId);
            showToast(`Deleted node ${nodeId}.`, 'info');
            activeRoute = null;
            if (visualizer) visualizer.setRoute(null);
            await syncNetwork();
            await syncSimulationState();
            await syncStatistics();
        } catch (e) {
            showToast(e.message, 'error');
        }
    });

    elements.btnDeleteSelectedLink.addEventListener('click', async () => {
        const linkId = elements.selTargetLink.value;
        if (!linkId) {
            showToast('Select a link in Target Link dropdown to delete.', 'warn');
            return;
        }
        if (!confirm(`Are you sure you want to delete link ${linkId}?`)) {
            return;
        }

        try {
            await API.deleteLink(linkId);
            showToast(`Deleted link ${linkId}.`, 'info');
            activeRoute = null;
            if (visualizer) visualizer.setRoute(null);
            await syncNetwork();
            await syncSimulationState();
            await syncStatistics();
        } catch (e) {
            showToast(e.message, 'error');
        }
    });

    // ---- Project Lifecycle (New / Reset) ----
    elements.btnNewProject.addEventListener('click', async () => {
        if (currentNetwork && currentNetwork.topologyModified) {
            if (!confirm('You have unsaved changes. Discard and create a new blank network?')) {
                return;
            }
        }
        try {
            await API.newProject();
            showToast('Created new blank project.', 'info');
            activeRoute = null;
            if (visualizer) visualizer.setRoute(null);
            elements.routeResultBox.innerHTML = `<span>No route calculated yet.</span>`;
            elements.routeResultBox.className = 'route-result-box';
            await syncNetwork();
            await syncSimulationState();
            await syncStatistics();
        } catch (e) {
            showToast(e.message, 'error');
        }
    });

    elements.btnResetCampusNetwork.addEventListener('click', async () => {
        if (!confirm('Reset current network to the default 6-node Sample Campus Network?')) {
            return;
        }
        try {
            await API.resetNetwork();
            showToast('Reset to Sample Campus Network.', 'info');
            activeRoute = null;
            if (visualizer) visualizer.setRoute(null);
            elements.routeResultBox.innerHTML = `<span>No route calculated yet.</span>`;
            elements.routeResultBox.className = 'route-result-box';
            await syncNetwork();
            await syncSimulationState();
            await syncStatistics();
        } catch (e) {
            showToast(e.message, 'error');
        }
    });

    // ---- Save Topology & Run Modal ----
    function openSaveModal() {
        if (currentNetwork) {
            elements.txtSaveTopoName.value = currentNetwork.activeTopologyName || 'Campus Network';
            elements.txtSaveTopoDesc.value = currentNetwork.activeTopologyDescription || '';
        }
        elements.modalSaveTopology.classList.add('active');
    }

    elements.btnSaveProject.addEventListener('click', openSaveModal);
    elements.btnOpenSaveModal.addEventListener('click', openSaveModal);

    elements.chkRecordSimulationRun.addEventListener('change', () => {
        elements.groupRunNotes.style.display = elements.chkRecordSimulationRun.checked ? 'block' : 'none';
    });

    elements.btnConfirmSave.addEventListener('click', async () => {
        const name = elements.txtSaveTopoName.value.trim();
        const desc = elements.txtSaveTopoDesc.value.trim();
        const overwrite = elements.chkOverwriteIfExists.checked;
        const recordRun = elements.chkRecordSimulationRun.checked;
        const notes = elements.txtSaveRunNotes.value.trim();

        if (!name) {
            showToast('Please enter a topology name.', 'warn');
            return;
        }

        try {
            const result = await API.saveTopology(
                currentNetwork ? currentNetwork.activeTopologyId : null,
                name,
                desc,
                overwrite,
                recordRun,
                notes
            );

            elements.modalSaveTopology.classList.remove('active');
            showToast(`Topology '${name}' saved successfully (ID: ${result.topologyId})!`, 'success');
            await syncNetwork();
            await syncSimulationState();
            await checkDatabaseStatus();
        } catch (e) {
            if (e.status === 409) {
                const proceed = confirm(`${e.message}\n\nWould you like to overwrite the existing topology?`);
                if (proceed) {
                    elements.chkOverwriteIfExists.checked = true;
                    elements.btnConfirmSave.click();
                }
            } else {
                showToast(e.message, 'error');
            }
        }
    });

    // Close Modals handler
    document.querySelectorAll('[data-close]').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const modalId = btn.getAttribute('data-close');
            const modal = document.getElementById(modalId);
            if (modal) modal.classList.remove('active');
        });
    });

    // ---- Topologies Page (View 3) ----
    async function loadTopologiesList() {
        elements.topologiesGrid.innerHTML = '<div style="color: var(--text-secondary); padding: 20px;">Loading saved topologies from MySQL...</div>';
        try {
            const topologies = await API.listTopologies();
            renderTopologiesGrid(topologies);
        } catch (e) {
            elements.topologiesGrid.innerHTML = `
                <div style="grid-column: 1 / -1; background: var(--bg-panel); border: 1px solid var(--status-error); border-radius: 8px; padding: 20px; color: var(--text-primary);">
                    <h4 style="color: var(--status-error); margin-bottom: 8px;">MySQL Database Connection Notice</h4>
                    <p style="font-size: 13px; color: var(--text-secondary);">${escapeHtml(e.message)}</p>
                    <p style="font-size: 12px; color: var(--text-muted); margin-top: 8px;">Make sure MySQL Server is running and <code>netroute_db</code> is initialized.</p>
                </div>
            `;
        }
    }

    function renderTopologiesGrid(topologies) {
        elements.topologiesGrid.innerHTML = '';
        if (!topologies || topologies.length === 0) {
            elements.topologiesGrid.innerHTML = '<div style="grid-column: 1 / -1; color: var(--text-secondary); padding: 40px; text-align: center;">No saved topologies found in database.</div>';
            return;
        }

        const filter = (elements.txtSearchTopologies.value || '').toLowerCase().trim();
        const filtered = topologies.filter(t => t.name.toLowerCase().includes(filter) || (t.description && t.description.toLowerCase().includes(filter)));

        filtered.forEach(topo => {
            const card = document.createElement('div');
            card.className = 'topology-card';
            card.innerHTML = `
                <div>
                    <div class="topo-header">
                        <span class="topo-title">${escapeHtml(topo.name)}</span>
                        <span class="topo-id">#${topo.id}</span>
                    </div>
                    <div class="topo-desc">${escapeHtml(topo.description || 'No description provided.')}</div>
                    <div class="topo-stats">
                        <span>Nodes: <strong>${topo.nodeCount}</strong></span>
                        <span>Links: <strong>${topo.linkCount}</strong></span>
                    </div>
                </div>
                <div class="topo-footer">
                    <span class="topo-date">${topo.updatedAt ? topo.updatedAt.replace('T', ' ').substring(0, 19) : ''}</span>
                    <div class="topo-btns">
                        <button class="btn-sm primary" data-load="${topo.id}">Load</button>
                        <button class="btn-sm" style="color: var(--status-error);" data-delete="${topo.id}">Delete</button>
                    </div>
                </div>
            `;
            elements.topologiesGrid.appendChild(card);
        });

        // Wire load and delete buttons
        elements.topologiesGrid.querySelectorAll('[data-load]').forEach(btn => {
            btn.addEventListener('click', async () => {
                const id = parseInt(btn.getAttribute('data-load'));
                try {
                    await API.loadTopology(id);
                    showToast(`Topology #${id} loaded successfully!`, 'success');
                    activeRoute = null;
                    if (visualizer) visualizer.setRoute(null);
                    switchView('simulatorView');
                    await syncNetwork();
                    await syncSimulationState();
                    await syncStatistics();
                } catch (e) {
                    showToast(e.message, 'error');
                }
            });
        });

        elements.topologiesGrid.querySelectorAll('[data-delete]').forEach(btn => {
            btn.addEventListener('click', async () => {
                const id = parseInt(btn.getAttribute('data-delete'));
                if (!confirm(`Are you sure you want to delete topology #${id} from MySQL?`)) {
                    return;
                }
                try {
                    await API.deleteTopology(id, true);
                    showToast(`Topology #${id} deleted.`, 'info');
                    loadTopologiesList();
                    await syncNetwork();
                } catch (e) {
                    showToast(e.message, 'error');
                }
            });
        });
    }

    elements.txtSearchTopologies.addEventListener('input', () => {
        loadTopologiesList();
    });

    // ---- Simulation History Page (View 4) ----
    async function loadSimulationHistoryList() {
        elements.runsTableBody.innerHTML = '<tr><td colspan="12" style="text-align: center; color: var(--text-secondary); padding: 30px;">Loading historical runs from MySQL...</td></tr>';
        try {
            const runs = await API.listSimulationRuns();
            renderRunsTable(runs);
        } catch (e) {
            elements.runsTableBody.innerHTML = `<tr><td colspan="12" style="text-align: center; color: var(--status-error); padding: 30px;">Error loading simulation runs: ${escapeHtml(e.message)}</td></tr>`;
        }
    }

    function renderRunsTable(runs) {
        elements.runsTableBody.innerHTML = '';
        if (!runs || runs.length === 0) {
            elements.runsTableBody.innerHTML = '<tr><td colspan="12" style="text-align: center; color: var(--text-secondary); padding: 30px;">No simulation runs recorded yet. Run a simulation and click "Save Topology & Run".</td></tr>';
            return;
        }

        const filter = (elements.txtSearchRuns.value || '').toLowerCase().trim();
        const filtered = runs.filter(r => 
            (r.topologyName && r.topologyName.toLowerCase().includes(filter)) ||
            String(r.id).includes(filter)
        );

        filtered.forEach(run => {
            const tr = document.createElement('tr');
            const ratioClass = run.deliveryRatio >= 90 ? 'badge-success' : (run.deliveryRatio >= 50 ? 'badge-warning' : 'badge-error');
            const dateStr = run.runTimestamp ? run.runTimestamp.replace('T', ' ').substring(0, 19) : '';

            tr.innerHTML = `
                <td><strong>#${run.id}</strong></td>
                <td><span style="font-weight: 600;">${escapeHtml(run.topologyName)}</span></td>
                <td><span style="color: var(--text-muted); font-size: 11px;">${dateStr}</span></td>
                <td>${run.simulationTicks}t</td>
                <td>${run.totalPackets}</td>
                <td style="color: var(--status-success);">${run.deliveredPackets}</td>
                <td style="color: var(--status-error);">${run.failedPackets}</td>
                <td style="color: var(--status-error);">${run.droppedPackets}</td>
                <td><span class="badge ${ratioClass}">${run.deliveryRatio.toFixed(1)}%</span></td>
                <td>${run.avgDeliveryDelay.toFixed(2)}t</td>
                <td>${run.linkFailures}</td>
                <td>${run.reroutes}</td>
            `;

            tr.addEventListener('click', () => openRunDetails(run));
            elements.runsTableBody.appendChild(tr);
        });
    }

    function openRunDetails(run) {
        elements.lblRunModalTitle.textContent = `Simulation Run #${run.id} — ${run.topologyName}`;
        elements.runDetailsBody.innerHTML = `
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 16px;">
                <div class="metric-tile">
                    <div class="metric-label">Execution Time</div>
                    <div style="font-size: 13px; font-weight: 600;">${run.runTimestamp ? run.runTimestamp.replace('T', ' ') : ''}</div>
                </div>
                <div class="metric-tile">
                    <div class="metric-label">Simulation Clock</div>
                    <div style="font-size: 13px; font-weight: 600;">${run.simulationTicks} ticks</div>
                </div>
                <div class="metric-tile">
                    <div class="metric-label">Packets Delivered</div>
                    <div style="font-size: 13px; font-weight: 600; color: var(--status-success);">${run.deliveredPackets} / ${run.totalPackets} (${run.deliveryRatio.toFixed(1)}%)</div>
                </div>
                <div class="metric-tile">
                    <div class="metric-label">Avg Delivery Delay</div>
                    <div style="font-size: 13px; font-weight: 600; color: var(--accent-blue);">${run.avgDeliveryDelay.toFixed(2)} ticks</div>
                </div>
                <div class="metric-tile">
                    <div class="metric-label">Inactive / Failed Links at Save</div>
                    <div style="font-size: 13px; font-weight: 600; color: var(--status-warning);">${run.linkFailures}</div>
                </div>
                <div class="metric-tile">
                    <div class="metric-label">Successful Dynamic Reroutes</div>
                    <div style="font-size: 13px; font-weight: 600; color: var(--accent-cyan);">${run.reroutes}</div>
                </div>
            </div>
            ${run.notes ? `
                <div style="background: var(--bg-input); border: 1px solid var(--border-subtle); padding: 12px; border-radius: 6px;">
                    <div class="metric-label" style="margin-bottom: 4px;">Scenario Notes</div>
                    <div style="font-size: 13px; color: var(--text-primary);">${escapeHtml(run.notes)}</div>
                </div>
            ` : ''}
        `;
        elements.modalRunDetails.classList.add('active');
    }

    elements.txtSearchRuns.addEventListener('input', () => {
        loadSimulationHistoryList();
    });
    elements.btnRefreshRuns.addEventListener('click', loadSimulationHistoryList);

    // Clear event log button
    elements.btnClearLog.addEventListener('click', () => {
        elements.terminalBox.innerHTML = '<div class="terminal-line terminal-time">Log cleared.</div>';
    });

    // ---- Initial Load Sequence ----
    (async function startup() {
        await checkDatabaseStatus();
        await syncNetwork();
        await syncSimulationState();
        await syncStatistics();
        startPolling();
    })();
});
