/**
 * NetRoute HTML5 Canvas Network Visualizer
 * High-performance 2D canvas renderer supporting:
 * - High-DPI / Retina screens
 * - Pan and zoom
 * - Interactive node dragging
 * - Dijkstra route glow highlights
 * - Failed link visualization
 * - Directed link arrowheads
 * - Real-time packet animation
 */
class NetworkVisualizer {
    constructor(canvasId) {
        this.canvas = document.getElementById(canvasId);
        if (!this.canvas) {
            throw new Error(`Canvas #${canvasId} not found`);
        }
        this.ctx = this.canvas.getContext('2d');

        // State
        this.nodes = [];
        this.links = [];
        this.highlightedRoute = null; // { nodeIds: [], linkIds: [] }
        this.packets = [];

        // Viewport Pan & Zoom
        this.panX = 0;
        this.panY = 0;
        this.zoom = 1.0;
        this.minZoom = 0.4;
        this.maxZoom = 2.5;

        // Interaction state
        this.isDragging = false;
        this.isPanning = false;
        this.draggedNode = null;
        this.dragOffsetX = 0;
        this.dragOffsetY = 0;
        this.panStartX = 0;
        this.panStartY = 0;
        this.hoveredNode = null;
        this.selectedNode = null;

        // Visual constants
        this.nodeWidth = 72;
        this.nodeHeight = 46;
        this.nodeRadius = 8;

        this.colors = {
            bg: '#0B1118',
            grid: '#162230',
            gridDot: '#1C2E42',
            linkNormal: '#4A647D',
            linkFailed: '#E65A5A',
            linkRoute: '#43C6D9',
            linkRouteGlow: 'rgba(67, 198, 217, 0.45)',
            textPrimary: '#E6EDF3',
            textSecondary: '#8B949E',
            computer: '#3B9AEE',
            router: '#2EC4B6',
            server: '#9E7AD2',
            packetGlow: '#FFD166'
        };

        // Animation loop
        this.animating = true;
        this.packetAnimProgress = 0;

        this.init();
    }

    init() {
        this.resize();
        window.addEventListener('resize', () => this.resize());
        this.bindEvents();
        this.startLoop();
    }

    resize() {
        const rect = this.canvas.parentElement.getBoundingClientRect();
        const dpr = window.devicePixelRatio || 1;
        this.canvas.width = rect.width * dpr;
        this.canvas.height = rect.height * dpr;
        this.canvas.style.width = `${rect.width}px`;
        this.canvas.style.height = `${rect.height}px`;
        this.ctx.scale(dpr, dpr);
        this.render();
    }

    setData(nodes, links, route = null, packets = []) {
        this.nodes = nodes || [];
        this.links = links || [];
        this.highlightedRoute = route;
        this.packets = packets || [];
        this.render();
    }

    setRoute(route) {
        this.highlightedRoute = route;
        this.render();
    }

    setPackets(packets) {
        this.packets = packets || [];
    }

    screenToWorld(screenX, screenY) {
        const rect = this.canvas.getBoundingClientRect();
        const clientX = screenX - rect.left;
        const clientY = screenY - rect.top;
        return {
            x: (clientX - this.panX) / this.zoom,
            y: (clientY - this.panY) / this.zoom
        };
    }

    findNodeAt(x, y) {
        const halfW = this.nodeWidth / 2;
        const halfH = this.nodeHeight / 2;
        for (let i = this.nodes.length - 1; i >= 0; i--) {
            const node = this.nodes[i];
            if (
                x >= node.x - halfW &&
                x <= node.x + halfW &&
                y >= node.y - halfH &&
                y <= node.y + halfH
            ) {
                return node;
            }
        }
        return null;
    }

    bindEvents() {
        this.canvas.addEventListener('mousedown', (e) => this.onMouseDown(e));
        window.addEventListener('mousemove', (e) => this.onMouseMove(e));
        window.addEventListener('mouseup', (e) => this.onMouseUp(e));
        this.canvas.addEventListener('wheel', (e) => this.onWheel(e), { passive: false });

        // Touch support for tablets/mobile
        this.canvas.addEventListener('touchstart', (e) => this.onTouchStart(e), { passive: false });
        this.canvas.addEventListener('touchmove', (e) => this.onTouchMove(e), { passive: false });
        this.canvas.addEventListener('touchend', (e) => this.onTouchEnd(e));
    }

    onMouseDown(e) {
        if (e.button === 0) { // Left click
            const pt = this.screenToWorld(e.clientX, e.clientY);
            const clickedNode = this.findNodeAt(pt.x, pt.y);

            if (clickedNode) {
                this.isDragging = true;
                this.draggedNode = clickedNode;
                this.dragOffsetX = clickedNode.x - pt.x;
                this.dragOffsetY = clickedNode.y - pt.y;
                this.selectedNode = clickedNode;
                if (window.onNodeSelected) {
                    window.onNodeSelected(clickedNode);
                }
            } else {
                this.isPanning = true;
                this.panStartX = e.clientX - this.panX;
                this.panStartY = e.clientY - this.panY;
                this.selectedNode = null;
            }
        } else if (e.button === 1 || e.button === 2) { // Middle or right click pans
            this.isPanning = true;
            this.panStartX = e.clientX - this.panX;
            this.panStartY = e.clientY - this.panY;
        }
    }

    onMouseMove(e) {
        const pt = this.screenToWorld(e.clientX, e.clientY);

        if (this.isDragging && this.draggedNode) {
            this.draggedNode.x = Math.round(pt.x + this.dragOffsetX);
            this.draggedNode.y = Math.round(pt.y + this.dragOffsetY);
            this.render();
        } else if (this.isPanning) {
            this.panX = e.clientX - this.panStartX;
            this.panY = e.clientY - this.panStartY;
            this.render();
        } else {
            const hovered = this.findNodeAt(pt.x, pt.y);
            if (hovered !== this.hoveredNode) {
                this.hoveredNode = hovered;
                this.canvas.style.cursor = hovered ? 'pointer' : 'grab';
                this.render();
            }
        }
    }

    onMouseUp(e) {
        if (this.isDragging && this.draggedNode) {
            // Commit position update to backend
            const node = this.draggedNode;
            this.isDragging = false;
            this.draggedNode = null;
            if (window.API) {
                window.API.updateNodePosition(node.id, node.x, node.y).catch(err => {
                    console.error('Failed to sync node position:', err);
                });
            }
        }
        this.isPanning = false;
    }

    onWheel(e) {
        e.preventDefault();
        const zoomFactor = e.deltaY < 0 ? 1.1 : 0.9;
        const rect = this.canvas.getBoundingClientRect();
        const mouseX = e.clientX - rect.left;
        const mouseY = e.clientY - rect.top;

        const newZoom = Math.max(this.minZoom, Math.min(this.maxZoom, this.zoom * zoomFactor));
        if (newZoom !== this.zoom) {
            this.panX = mouseX - (mouseX - this.panX) * (newZoom / this.zoom);
            this.panY = mouseY - (mouseY - this.panY) * (newZoom / this.zoom);
            this.zoom = newZoom;
            this.render();
        }
    }

    onTouchStart(e) {
        if (e.touches.length === 1) {
            const touch = e.touches[0];
            const pt = this.screenToWorld(touch.clientX, touch.clientY);
            const clicked = this.findNodeAt(pt.x, pt.y);
            if (clicked) {
                this.isDragging = true;
                this.draggedNode = clicked;
                this.dragOffsetX = clicked.x - pt.x;
                this.dragOffsetY = clicked.y - pt.y;
                e.preventDefault();
            } else {
                this.isPanning = true;
                this.panStartX = touch.clientX - this.panX;
                this.panStartY = touch.clientY - this.panY;
            }
        }
    }

    onTouchMove(e) {
        if (e.touches.length === 1) {
            const touch = e.touches[0];
            const pt = this.screenToWorld(touch.clientX, touch.clientY);
            if (this.isDragging && this.draggedNode) {
                this.draggedNode.x = Math.round(pt.x + this.dragOffsetX);
                this.draggedNode.y = Math.round(pt.y + this.dragOffsetY);
                this.render();
                e.preventDefault();
            } else if (this.isPanning) {
                this.panX = touch.clientX - this.panStartX;
                this.panY = touch.clientY - this.panStartY;
                this.render();
                e.preventDefault();
            }
        }
    }

    onTouchEnd(e) {
        if (this.isDragging && this.draggedNode) {
            const node = this.draggedNode;
            this.isDragging = false;
            this.draggedNode = null;
            if (window.API) {
                window.API.updateNodePosition(node.id, node.x, node.y).catch(console.error);
            }
        }
        this.isPanning = false;
    }

    zoomIn() {
        this.zoom = Math.min(this.maxZoom, this.zoom * 1.2);
        this.render();
    }

    zoomOut() {
        this.zoom = Math.max(this.minZoom, this.zoom / 1.2);
        this.render();
    }

    resetView() {
        this.zoom = 1.0;
        this.panX = 0;
        this.panY = 0;
        this.render();
    }

    startLoop() {
        const loop = () => {
            this.packetAnimProgress = (this.packetAnimProgress + 0.015) % 1.0;
            if (this.packets.length > 0) {
                this.render();
            }
            requestAnimationFrame(loop);
        };
        requestAnimationFrame(loop);
    }

    render() {
        const ctx = this.ctx;
        const rect = this.canvas.getBoundingClientRect();
        const width = rect.width;
        const height = rect.height;

        ctx.save();
        ctx.clearRect(0, 0, width, height);

        // Apply pan and zoom
        ctx.translate(this.panX, this.panY);
        ctx.scale(this.zoom, this.zoom);

        // 1. Draw Technical Grid
        this.drawGrid(ctx, width, height);

        // Map nodes by ID for fast link coordinate lookup
        const nodeMap = new Map();
        for (const n of this.nodes) {
            nodeMap.set(n.id, n);
        }

        // 2. Draw Links
        for (const link of this.links) {
            const src = nodeMap.get(link.sourceId);
            const dst = nodeMap.get(link.destinationId);
            if (src && dst) {
                this.drawLink(ctx, link, src, dst);
            }
        }

        // 3. Draw Packets in-flight
        this.drawPackets(ctx, nodeMap);

        // 4. Draw Nodes
        for (const node of this.nodes) {
            this.drawNode(ctx, node);
        }

        ctx.restore();
    }

    drawGrid(ctx, width, height) {
        const startX = -this.panX / this.zoom;
        const startY = -this.panY / this.zoom;
        const endX = startX + width / this.zoom;
        const endY = startY + height / this.zoom;

        const gridSize = 40;
        const firstX = Math.floor(startX / gridSize) * gridSize;
        const firstY = Math.floor(startY / gridSize) * gridSize;

        ctx.strokeStyle = this.colors.grid;
        ctx.lineWidth = 1;
        ctx.beginPath();

        for (let x = firstX; x <= endX; x += gridSize) {
            ctx.moveTo(x, startY);
            ctx.lineTo(x, endY);
        }
        for (let y = firstY; y <= endY; y += gridSize) {
            ctx.moveTo(startX, y);
            ctx.lineTo(endX, y);
        }
        ctx.stroke();
    }

    drawLink(ctx, link, src, dst) {
        const isRouteLink = this.highlightedRoute &&
            this.highlightedRoute.linkIds &&
            this.highlightedRoute.linkIds.includes(link.id);

        const isFailed = !link.active;

        ctx.save();

        if (isRouteLink && !isFailed) {
            // Glowing cyan path
            ctx.strokeStyle = this.colors.linkRoute;
            ctx.lineWidth = 4;
            ctx.shadowColor = this.colors.linkRouteGlow;
            ctx.shadowBlur = 14;
        } else if (isFailed) {
            // Failed link: Red dashed line
            ctx.strokeStyle = this.colors.linkFailed;
            ctx.lineWidth = 2.5;
            ctx.setLineDash([8, 5]);
            ctx.shadowColor = 'rgba(230, 90, 90, 0.4)';
            ctx.shadowBlur = 8;
        } else {
            // Normal link
            ctx.strokeStyle = this.colors.linkNormal;
            ctx.lineWidth = 2;
        }

        // Draw line
        ctx.beginPath();
        ctx.moveTo(src.x, src.y);
        ctx.lineTo(dst.x, dst.y);
        ctx.stroke();

        // Draw directed arrow if directed
        if (link.directed) {
            this.drawArrow(ctx, src.x, src.y, dst.x, dst.y, ctx.strokeStyle);
        }

        ctx.restore();

        // Draw link cost & capacity badge at midpoint
        this.drawLinkBadge(ctx, link, src, dst, isFailed, isRouteLink);
    }

    drawArrow(ctx, fromX, fromY, toX, toY, color) {
        const headLen = 10;
        const angle = Math.atan2(toY - fromY, toX - fromX);
        const midX = (fromX + toX) / 2 + Math.cos(angle) * 14;
        const midY = (fromY + toY) / 2 + Math.sin(angle) * 14;

        ctx.save();
        ctx.fillStyle = color;
        ctx.beginPath();
        ctx.moveTo(midX, midY);
        ctx.lineTo(midX - headLen * Math.cos(angle - Math.PI / 6), midY - headLen * Math.sin(angle - Math.PI / 6));
        ctx.lineTo(midX - headLen * Math.cos(angle + Math.PI / 6), midY - headLen * Math.sin(angle + Math.PI / 6));
        ctx.closePath();
        ctx.fill();
        ctx.restore();
    }

    drawLinkBadge(ctx, link, src, dst, isFailed, isRouteLink) {
        const midX = (src.x + dst.x) / 2;
        const midY = (src.y + dst.y) / 2;

        const text = isFailed ? `FAILED` : `c=${link.cost} d=${link.delay}t`;
        ctx.font = '10px "JetBrains Mono", monospace';
        const textWidth = ctx.measureText(text).width;
        const badgeW = textWidth + 12;
        const badgeH = 18;

        ctx.save();
        ctx.beginPath();
        this.roundRect(ctx, midX - badgeW / 2, midY - badgeH / 2, badgeW, badgeH, 4);

        if (isFailed) {
            ctx.fillStyle = '#2A1212';
            ctx.strokeStyle = this.colors.linkFailed;
        } else if (isRouteLink) {
            ctx.fillStyle = '#0E242B';
            ctx.strokeStyle = this.colors.linkRoute;
        } else {
            ctx.fillStyle = '#101923';
            ctx.strokeStyle = this.colors.linkNormal;
        }

        ctx.fill();
        ctx.lineWidth = 1;
        ctx.stroke();

        ctx.fillStyle = isFailed ? this.colors.linkFailed : (isRouteLink ? this.colors.linkRoute : '#8B949E');
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText(text, midX, midY);
        ctx.restore();
    }

    drawPackets(ctx, nodeMap) {
        if (!this.packets || this.packets.length === 0) return;

        for (const pkt of this.packets) {
            if (pkt.status === 'IN_TRANSIT' && pkt.nextLinkId) {
                const link = this.links.find(l => l.id === pkt.nextLinkId);
                if (link) {
                    const src = nodeMap.get(pkt.currentNodeId);
                    const dstId = link.sourceId === pkt.currentNodeId ? link.destinationId : link.sourceId;
                    const dst = nodeMap.get(dstId);

                    if (src && dst) {
                        const px = src.x + (dst.x - src.x) * this.packetAnimProgress;
                        const py = src.y + (dst.y - src.y) * this.packetAnimProgress;

                        ctx.save();
                        ctx.fillStyle = this.colors.packetGlow;
                        ctx.shadowColor = '#FFD166';
                        ctx.shadowBlur = 12;
                        ctx.beginPath();
                        ctx.arc(px, py, 6, 0, Math.PI * 2);
                        ctx.fill();

                        ctx.fillStyle = '#0B1118';
                        ctx.font = 'bold 8px sans-serif';
                        ctx.textAlign = 'center';
                        ctx.textBaseline = 'middle';
                        ctx.fillText(pkt.id, px, py);
                        ctx.restore();
                    }
                }
            }
        }
    }

    drawNode(ctx, node) {
        const x = node.x;
        const y = node.y;
        const w = this.nodeWidth;
        const h = this.nodeHeight;
        const halfW = w / 2;
        const halfH = h / 2;

        const isHovered = this.hoveredNode && this.hoveredNode.id === node.id;
        const isSelected = this.selectedNode && this.selectedNode.id === node.id;
        const isRouteNode = this.highlightedRoute &&
            this.highlightedRoute.nodeIds &&
            this.highlightedRoute.nodeIds.includes(node.id);

        let typeColor = this.colors.computer;
        let iconSymbol = '💻';
        if (node.type === 'ROUTER') {
            typeColor = this.colors.router;
            iconSymbol = '⇄';
        } else if (node.type === 'SERVER') {
            typeColor = this.colors.server;
            iconSymbol = '🖪';
        }

        ctx.save();

        // Selection / Route Glow
        if (isSelected || isRouteNode) {
            ctx.shadowColor = this.colors.linkRoute;
            ctx.shadowBlur = 16;
            ctx.strokeStyle = this.colors.linkRoute;
            ctx.lineWidth = 2.5;
        } else if (isHovered) {
            ctx.shadowColor = typeColor;
            ctx.shadowBlur = 10;
            ctx.strokeStyle = typeColor;
            ctx.lineWidth = 2;
        } else {
            ctx.strokeStyle = '#2A3A4D';
            ctx.lineWidth = 1.5;
        }

        // Node Body (Rounded Rectangle)
        ctx.fillStyle = '#151F2B';
        ctx.beginPath();
        this.roundRect(ctx, x - halfW, y - halfH, w, h, this.nodeRadius);
        ctx.fill();
        ctx.stroke();

        // Header accent bar
        ctx.fillStyle = typeColor;
        ctx.beginPath();
        this.roundRect(ctx, x - halfW, y - halfH, w, 5, [this.nodeRadius, this.nodeRadius, 0, 0]);
        ctx.fill();

        // Node ID (Bold)
        ctx.fillStyle = '#FFFFFF';
        ctx.font = 'bold 12px "Inter", sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText(node.id, x, y - 4);

        // Node Name (Small)
        ctx.fillStyle = '#8B949E';
        ctx.font = '9px "Inter", sans-serif';
        const truncatedName = node.name.length > 12 ? node.name.substring(0, 10) + '..' : node.name;
        ctx.fillText(truncatedName, x, y + 11);

        ctx.restore();
    }

    roundRect(ctx, x, y, width, height, radius) {
        if (typeof radius === 'number') {
            radius = { tl: radius, tr: radius, br: radius, bl: radius };
        } else if (Array.isArray(radius)) {
            radius = { tl: radius[0], tr: radius[1], br: radius[2], bl: radius[3] };
        }
        ctx.moveTo(x + radius.tl, y);
        ctx.lineTo(x + width - radius.tr, y);
        ctx.quadraticCurveTo(x + width, y, x + width, y + radius.tr);
        ctx.lineTo(x + width, y + height - radius.br);
        ctx.quadraticCurveTo(x + width, y + height, x + width - radius.br, y + height);
        ctx.lineTo(x + radius.bl, y + height);
        ctx.quadraticCurveTo(x, y + height, x, y + height - radius.bl);
        ctx.lineTo(x, y + radius.tl);
        ctx.quadraticCurveTo(x, y, x + radius.tl, y);
        ctx.closePath();
    }
}

window.NetworkVisualizer = NetworkVisualizer;
