class AuctionHouse {
    constructor() {
        this.auctions = [];
        this.currentPage = 1;
        this.itemsPerPage = 12;
        this.filteredAuctions = [];
        this.init();
    }

    init() {
        this.loadAuctions();
        this.setupEventListeners();
        this.setupAutoRefresh();
    }

    setupEventListeners() {
        // Search input
        document.getElementById('searchInput').addEventListener('input', (e) => {
            this.filterAuctions();
        });

        // Sort select
        document.getElementById('sortSelect').addEventListener('change', (e) => {
            this.sortAuctions();
        });

        // Refresh button
        document.getElementById('refreshBtn').addEventListener('click', () => {
            this.loadAuctions();
        });

        // Pagination
        document.getElementById('prevPage').addEventListener('click', () => {
            if (this.currentPage > 1) {
                this.currentPage--;
                this.renderAuctions();
            }
        });

        document.getElementById('nextPage').addEventListener('click', () => {
            const totalPages = Math.ceil(this.filteredAuctions.length / this.itemsPerPage);
            if (this.currentPage < totalPages) {
                this.currentPage++;
                this.renderAuctions();
            }
        });

        // Modal
        const modal = document.getElementById('itemModal');
        const closeBtn = document.querySelector('.close-btn');
        const backdrop = document.querySelector('.modal-backdrop');

        closeBtn.addEventListener('click', () => {
            modal.style.display = 'none';
        });

        backdrop.addEventListener('click', () => {
            modal.style.display = 'none';
        });

        // Keyboard navigation
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && modal.style.display === 'block') {
                modal.style.display = 'none';
            }
        });
    }

    setupAutoRefresh() {
        // Auto-refresh every 30 seconds
        setInterval(() => {
            this.loadAuctions();
        }, 30000);
    }

    async loadAuctions() {
        try {
            const response = await fetch('/api/auctions');
            if (!response.ok) {
                throw new Error('Failed to load auctions');
            }
            this.auctions = await response.json();
            this.filterAuctions();
            this.updateStats();
        } catch (error) {
            console.error('Error loading auctions:', error);
            document.getElementById('auctionGrid').innerHTML =
                '<div class="loading-screen">❌ Failed to load auctions. Please try again.</div>';
        }
    }

    filterAuctions() {
        const searchTerm = document.getElementById('searchInput').value.toLowerCase();

        this.filteredAuctions = this.auctions.filter(auction => {
            const itemName = this.getItemDisplayName(auction.item).toLowerCase();
            const sellerName = auction.sellerName.toLowerCase();
            const itemType = auction.item.type.toLowerCase();

            return itemName.includes(searchTerm) ||
                   sellerName.includes(searchTerm) ||
                   itemType.includes(searchTerm);
        });

        this.currentPage = 1;
        this.sortAuctions();
    }

    sortAuctions() {
        const sortBy = document.getElementById('sortSelect').value;

        this.filteredAuctions.sort((a, b) => {
            switch (sortBy) {
                case 'newest':
                    return b.listTime - a.listTime;
                case 'oldest':
                    return a.listTime - b.listTime;
                case 'price-low':
                    return a.price - b.price;
                case 'price-high':
                    return b.price - a.price;
                default:
                    return 0;
            }
        });

        this.renderAuctions();
    }

    renderAuctions() {
        const grid = document.getElementById('auctionGrid');

        if (this.filteredAuctions.length === 0) {
            grid.innerHTML = '<div class="loading-screen">📦 No auctions found matching your search</div>';
            return;
        }

        const startIndex = (this.currentPage - 1) * this.itemsPerPage;
        const endIndex = startIndex + this.itemsPerPage;
        const pageAuctions = this.filteredAuctions.slice(startIndex, endIndex);

        grid.innerHTML = pageAuctions.map(auction => this.createAuctionCard(auction)).join('');

        // Add click listeners to auction cards
        document.querySelectorAll('.auction-item').forEach(card => {
            card.addEventListener('click', () => {
                const auctionId = parseInt(card.dataset.auctionId);
                this.showItemModal(auctionId);
            });
        });

        this.updatePagination();
        this.updateItemCount();
    }

    createAuctionCard(auction) {
        const itemName = this.getItemDisplayName(auction.item);
        const itemType = this.formatItemType(auction.item.type);
        const itemIcon = this.getItemImageUrl(auction.item.type);
        const timeAgo = this.formatTimeAgo(auction.listTime);

        let loreHtml = '';
        if (auction.item.lore && auction.item.lore.length > 0) {
            loreHtml = `<div class="item-lore">${auction.item.lore.slice(0, 2).join('<br>')}</div>`;
        }

        let enchantmentsHtml = '';
        if (auction.item.enchantments && auction.item.enchantments.length > 0) {
            enchantmentsHtml = `<div class="item-enchantments">
                ${auction.item.enchantments.slice(0, 3).map(ench =>
                    `<span class="enchantment">${this.formatEnchantment(ench)}</span>`
                ).join('')}
            </div>`;
        }

        return `
            <div class="auction-item" data-auction-id="${auction.id}">
                <div class="auction-id">#${auction.id}</div>
                <div class="item-header">
                    <img src="${itemIcon}" alt="${itemName}" class="item-icon"
                         onerror="this.src='https://static.minecraftitemids.com/32/grass_block.png'">
                    <div class="item-info">
                        <h3>${itemName}</h3>
                        <div class="item-type">${itemType}</div>
                    </div>
                </div>
                <div class="item-details">
                    <div class="detail-row">
                        <span class="label">💰 Price:</span>
                        <span class="price">$${auction.price.toFixed(2)}</span>
                    </div>
                    <div class="detail-row">
                        <span class="label">👤 Seller:</span>
                        <span class="seller">${auction.sellerName}</span>
                    </div>
                    <div class="detail-row">
                        <span class="label">📦 Amount:</span>
                        <span>${auction.item.amount}</span>
                    </div>
                    <div class="detail-row">
                        <span class="label">⏰ Listed:</span>
                        <span>${timeAgo}</span>
                    </div>
                </div>
                ${loreHtml}
                ${enchantmentsHtml}
            </div>
        `;
    }

    showItemModal(auctionId) {
        const auction = this.auctions.find(a => a.id === auctionId);
        if (!auction) return;

        const modal = document.getElementById('itemModal');
        const itemName = this.getItemDisplayName(auction.item);
        const itemType = this.formatItemType(auction.item.type);
        const itemIcon = this.getItemImageUrl(auction.item.type);

        // Update modal content
        document.getElementById('modalItemIcon').src = itemIcon;
        document.getElementById('modalItemName').textContent = itemName;
        document.getElementById('modalItemType').textContent = itemType;
        document.getElementById('modalSeller').textContent = auction.sellerName;
        document.getElementById('modalPrice').textContent = `$${auction.price.toFixed(2)}`;
        document.getElementById('modalAmount').textContent = auction.item.amount;
        document.getElementById('modalListTime').textContent = new Date(auction.listTime).toLocaleString();
        document.getElementById('modalExpireTime').textContent = new Date(auction.expireTime).toLocaleString();
        document.getElementById('modalItemId').textContent = auction.id;

        // Lore
        const loreContainer = document.getElementById('modalLore');
        if (auction.item.lore && auction.item.lore.length > 0) {
            loreContainer.innerHTML = `
                <h4 style="color: #f39c12; margin-bottom: 10px;">📜 Item Description:</h4>
                ${auction.item.lore.map(line => `<div style="margin-bottom: 5px;">${line}</div>`).join('')}
            `;
            loreContainer.style.display = 'block';
        } else {
            loreContainer.style.display = 'none';
        }

        // Enchantments
        const enchantContainer = document.getElementById('modalEnchantments');
        if (auction.item.enchantments && auction.item.enchantments.length > 0) {
            enchantContainer.innerHTML = `
                <h4 style="color: #9b59b6; margin-bottom: 15px;">✨ Enchantments:</h4>
                <div style="display: flex; flex-wrap: wrap; gap: 8px;">
                    ${auction.item.enchantments.map(ench =>
                        `<span class="enchantment">${this.formatEnchantment(ench)}</span>`
                    ).join('')}
                </div>
            `;
            enchantContainer.style.display = 'block';
        } else {
            enchantContainer.style.display = 'none';
        }

        modal.style.display = 'block';
    }

    updateStats() {
        const totalItems = this.auctions.length;
        const avgPrice = totalItems > 0 ?
            this.auctions.reduce((sum, auction) => sum + auction.price, 0) / totalItems : 0;

        // Count unique sellers
        const uniqueSellers = new Set(this.auctions.map(auction => auction.sellerName)).size;

        document.getElementById('totalItems').textContent = totalItems;
        document.getElementById('avgPrice').textContent = `$${avgPrice.toFixed(2)}`;
        document.getElementById('activeSellers').textContent = uniqueSellers;
    }

    updatePagination() {
        const totalPages = Math.ceil(this.filteredAuctions.length / this.itemsPerPage);

        document.getElementById('prevPage').disabled = this.currentPage <= 1;
        document.getElementById('nextPage').disabled = this.currentPage >= totalPages;
        document.getElementById('pageInfo').textContent = `Page ${this.currentPage} of ${Math.max(1, totalPages)}`;
    }

    updateItemCount() {
        const showing = Math.min(this.currentPage * this.itemsPerPage, this.filteredAuctions.length);
        const startIndex = (this.currentPage - 1) * this.itemsPerPage + 1;
        document.getElementById('itemCount').textContent =
            `Showing ${startIndex}-${showing} of ${this.filteredAuctions.length} items`;
    }

    getItemDisplayName(item) {
        if (item.displayName && item.displayName.trim() !== '') {
            return item.displayName;
        }
        return this.formatItemType(item.type);
    }

    formatItemType(type) {
        return type.toLowerCase()
            .replace(/_/g, ' ')
            .replace(/\b\w/g, l => l.toUpperCase());
    }

    getItemImageUrl(type) {
        // Convert item type to lowercase and use the minecraftitemids.com URL format
        const formattedType = type.toLowerCase();
        return `https://static.minecraftitemids.com/32/${formattedType}.png`;
    }

    formatEnchantment(enchantment) {
        const name = enchantment.type.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase());
        return `${name} ${this.toRoman(enchantment.level)}`;
    }

    toRoman(num) {
        const romanNumerals = {
            1: 'I', 2: 'II', 3: 'III', 4: 'IV', 5: 'V',
            6: 'VI', 7: 'VII', 8: 'VIII', 9: 'IX', 10: 'X'
        };
        return romanNumerals[num] || num.toString();
    }

    formatTimeAgo(timestamp) {
        const now = Date.now();
        const diff = now - timestamp;
        const minutes = Math.floor(diff / 60000);
        const hours = Math.floor(minutes / 60);
        const days = Math.floor(hours / 24);

        if (days > 0) return `${days}d ago`;
        if (hours > 0) return `${hours}h ago`;
        if (minutes > 0) return `${minutes}m ago`;
        return 'Just now';
    }

    formatTimeUntil(timestamp) {
        const now = Date.now();
        const diff = timestamp - now;
        const minutes = Math.floor(diff / 60000);
        const hours = Math.floor(minutes / 60);
        const days = Math.floor(hours / 24);

        if (diff <= 0) return 'Expired';
        if (days > 0) return `${days}d left`;
        if (hours > 0) return `${hours}h left`;
        if (minutes > 0) return `${minutes}m left`;
        return 'Expires soon';
    }
}

// Global function for copy button
function copyCommand() {
    const commandText = document.querySelector('.command-display code').textContent;
    navigator.clipboard.writeText(commandText).then(() => {
        const copyBtn = document.querySelector('.copy-btn');
        const originalText = copyBtn.textContent;
        copyBtn.textContent = '✅ Copied!';
        setTimeout(() => {
            copyBtn.textContent = originalText;
        }, 2000);
    }).catch(() => {
        // Fallback for older browsers
        const textArea = document.createElement('textarea');
        textArea.value = commandText;
        document.body.appendChild(textArea);
        textArea.select();
        document.execCommand('copy');
        document.body.removeChild(textArea);

        const copyBtn = document.querySelector('.copy-btn');
        const originalText = copyBtn.textContent;
        copyBtn.textContent = '✅ Copied!';
        setTimeout(() => {
            copyBtn.textContent = originalText;
        }, 2000);
    });
}

// Initialize the auction house when the page loads
document.addEventListener('DOMContentLoaded', () => {
    new AuctionHouse();
});