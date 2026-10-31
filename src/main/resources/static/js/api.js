/**
 * NetRoute Web API Client
 * Encapsulates all REST communication with the Java backend.
 */
const API = {
    async request(url, options = {}) {
        const defaultHeaders = {
            'Accept': 'application/json',
            'Content-Type': 'application/json'
        };

        const config = {
            ...options,
            headers: {
                ...defaultHeaders,
                ...options.headers
            }
        };

        try {
            const response = await fetch(url, config);
            const json = await response.json().catch(() => null);

            if (!response.ok) {
                const errorMsg = (json && (json.error || json.message)) 
                    ? (json.error || json.message) 
                    : `HTTP ${response.status}: ${response.statusText}`;
                const err = new Error(errorMsg);
                err.status = response.status;
                err.data = json;
                throw err;
            }

            return json ? json.data : null;
        } catch (error) {
            console.error(`API Error [${options.method || 'GET'} ${url}]:`, error);
            throw error;
        }
    },

    // ---- Network Endpoints ----
    async getNetwork() {
        return this.request('/api/network');
    },

    async resetNetwork() {
        return this.request('/api/network/reset', { method: 'POST' });
    },

    async newProject() {
        return this.request('/api/network/new-project', { method: 'POST' });
    },

    async randomizeCosts() {
        return this.request('/api/network/randomize-costs', { method: 'POST' });
    },

    async addNode(type, name, x = null, y = null) {
        return this.request('/api/network/node', {
            method: 'POST',
            body: JSON.stringify({ type, name, x, y })
        });
    },

    async deleteNode(nodeId) {
        return this.request(`/api/network/node?id=${encodeURIComponent(nodeId)}`, {
            method: 'DELETE'
        });
    },

    async updateNodePosition(nodeId, x, y) {
        return this.request(`/api/network/node/position?id=${encodeURIComponent(nodeId)}`, {
            method: 'POST',
            body: JSON.stringify({ x, y })
        });
    },

    async connectNodes(sourceId, destinationId, cost, delay = 2.0, capacity = 4, directed = false) {
        return this.request('/api/network/link', {
            method: 'POST',
            body: JSON.stringify({ sourceId, destinationId, cost, delay, capacity, directed })
        });
    },

    async deleteLink(linkId) {
        return this.request(`/api/network/link?id=${encodeURIComponent(linkId)}`, {
            method: 'DELETE'
        });
    },

    // ---- Routing Endpoints ----
    async calculateRoute(sourceId, destinationId) {
        return this.request('/api/route', {
            method: 'POST',
            body: JSON.stringify({ sourceId, destinationId })
        });
    },

    // ---- Simulation Endpoints ----
    async getSimulationState() {
        return this.request('/api/simulation/state');
    },

    async submitPacket(sourceId, destinationId, payloadSize = 64) {
        return this.request('/api/simulation/packet', {
            method: 'POST',
            body: JSON.stringify({ sourceId, destinationId, payloadSize })
        });
    },

    async stepSimulation() {
        return this.request('/api/simulation/step', { method: 'POST' });
    },

    async runSimulation() {
        return this.request('/api/simulation/run', { method: 'POST' });
    },

    async resetSimulation() {
        return this.request('/api/simulation/reset', { method: 'POST' });
    },

    async failLink(linkId) {
        return this.request('/api/simulation/fail-link', {
            method: 'POST',
            body: JSON.stringify({ linkId })
        });
    },

    async restoreLink(linkId) {
        return this.request('/api/simulation/restore-link', {
            method: 'POST',
            body: JSON.stringify({ linkId })
        });
    },

    async getEvents() {
        return this.request('/api/simulation/events');
    },

    // ---- Statistics Endpoints ----
    async getStatistics() {
        return this.request('/api/statistics');
    },

    // ---- Database & History Endpoints ----
    async listTopologies(includeArchived = false) {
        return this.request(`/api/history/topologies?includeArchived=${includeArchived}`);
    },

    async saveTopology(topologyId, name, description, overwrite = false, recordRun = false, runNotes = '') {
        return this.request('/api/history/topologies/save', {
            method: 'POST',
            body: JSON.stringify({
                topologyId,
                name,
                description,
                overwrite,
                recordRun,
                runNotes
            })
        });
    },

    async loadTopology(topologyId) {
        return this.request('/api/history/topologies/load', {
            method: 'POST',
            body: JSON.stringify({ topologyId })
        });
    },

    async deleteTopology(topologyId, permanent = false) {
        return this.request(`/api/history/topologies?id=${encodeURIComponent(topologyId)}&permanent=${permanent}`, {
            method: 'DELETE'
        });
    },

    async listSimulationRuns() {
        return this.request('/api/history/runs');
    },

    async getDbStatus() {
        return this.request('/api/db/status');
    }
};

window.API = API;
