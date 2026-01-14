// ========================================
// 전역 변수
// ========================================
const API_BASE_URL = 'http://localhost:8080/api';
let currentProducts = [];
let currentProductId = null;
let myPoints = 0;

// ========================================
// 페이지 로드 시 실행
// ========================================
document.addEventListener('DOMContentLoaded', function () {
    const token = localStorage.getItem('accessToken');

    if (!token) {
        alert('로그인이 필요합니다.');
        window.location.href = '/login';
        return;
    }

    fetchPointShop(token);
});

// ========================================
// 포인트 상점 데이터 로드
// ========================================
async function fetchPointShop(token) {
    if (!token) {
        alert('로그인이 필요합니다.');
        window.location.href = '/auth/login';
        return;
    }

    try {
        const response = await fetch('/api/points/shop', {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });

        if (response.status === 401) {
            alert('로그인이 만료되었습니다.');
            localStorage.removeItem('accessToken');
            window.location.href = '/auth/login';
            return;
        }

        if (!response.ok) {
            throw new Error('상품 목록을 불러올 수 없습니다.');
        }

        const result = await response.json();
        console.log('포인트 상점 응답:', result);

        const data = result.data;

        // 전역 변수 저장
        myPoints = data.myPoints || 0;
        currentProducts = data.products || [];

        // 내 포인트 표시
        document.getElementById('myPoints').textContent =
            `${myPoints.toLocaleString()} P`;

        // 상품 목록 표시
        displayProducts(data.products);

    } catch (error) {
        console.error('Error:', error);
        alert('상품 목록을 불러오는데 실패했습니다.');
    }
}

// ========================================
// 상품 목록 표시
// ========================================
function displayProducts(products) {
    const productsGrid = document.getElementById('productsGrid');

    if (!products || products.length === 0) {
        productsGrid.innerHTML = `
            <div style="grid-column: 1 / -1; padding: 40px; text-align: center; color: #999;">
                현재 교환 가능한 상품이 없습니다.
            </div>
        `;
        return;
    }

    productsGrid.innerHTML = products.map(product => {
        // SVG 기본 이미지
        const defaultImage = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="200" height="200"%3E%3Crect fill="%23f0f0f0" width="200" height="200"/%3E%3Ctext x="50%25" y="50%25" dominant-baseline="middle" text-anchor="middle" font-family="sans-serif" font-size="16" fill="%23999"%3E상품 이미지%3C/text%3E%3C/svg%3E';

        const imageUrl = (product.productImageUrl &&
            !product.productImageUrl.includes('placeholder'))
            ? product.productImageUrl
            : defaultImage;

        return `
            <div class="product-item" onclick="showProductDetail(${product.productId})">
                <img src="${imageUrl}" 
                     alt="${product.productName}" 
                     class="product-image"
                     onerror="this.onerror=null; this.src='${defaultImage}';">
                <div class="product-info">
                    <div class="product-name">${product.productName}</div>
                    <div class="product-description">${product.productDescription || ''}</div>
                    <div class="product-points">${product.requiredPoint.toLocaleString()} P</div>
                    <button class="btn-exchange-item" onclick="event.stopPropagation(); exchangeProduct(${product.productId})">
                        교환
                    </button>
                </div>
            </div>
        `;
    }).join('');
}

// ========================================
// 상품 상세 조회
// ========================================
async function showProductDetail(productId) {
    const token = localStorage.getItem('accessToken');
    currentProductId = productId;

    try {
        const response = await fetch(`${API_BASE_URL}/points/shop/${productId}`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });

        const result = await response.json();

        if (result.success && result.data) {
            showProductModal(result.data);
        } else {
            alert(result.message || '상품 상세 정보를 불러오는데 실패했습니다.');
        }
    } catch (error) {
        console.error('상품 상세 조회 오류:', error);
        alert('상품 상세 정보를 불러오는데 오류가 발생했습니다.');
    }
}

// ========================================
// 상품 상세 모달 표시
// ========================================
function showProductModal(product) {
    const modal = document.getElementById('productModal');
    const modalImage = document.getElementById('modalImage');
    const modalProductName = document.getElementById('modalProductName');
    const modalProductDescription = document.getElementById('modalProductDescription');
    const modalRequiredPoints = document.getElementById('modalRequiredPoints');
    const btnExchange = document.getElementById('btnExchange');

    const defaultImage = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="300" height="300"%3E%3Crect fill="%23f0f0f0" width="300" height="300"/%3E%3Ctext x="50%25" y="50%25" dominant-baseline="middle" text-anchor="middle" font-family="sans-serif" font-size="18" fill="%23999"%3E상품 이미지%3C/text%3E%3C/svg%3E';

    if (modalImage) {
        modalImage.src = (product.productImageUrl &&
            !product.productImageUrl.includes('placeholder'))
            ? product.productImageUrl
            : defaultImage;
        modalImage.onerror = function () {
            this.onerror = null;
            this.src = defaultImage;
        };
    }

    if (modalProductName) {
        modalProductName.textContent = product.productName;
    }

    if (modalProductDescription) {
        modalProductDescription.textContent = product.productDescription || '상품 설명이 없습니다.';
    }

    if (modalRequiredPoints) {
        modalRequiredPoints.textContent = `${product.requiredPoint.toLocaleString()} P`;
    }

    if (btnExchange) {
        btnExchange.onclick = function () {
            exchangeProduct(product.productId);
        };
    }

    if (modal) {
        modal.style.display = 'flex';
        document.body.style.overflow = 'hidden';
    }
}

// ========================================
// 상품 상세 모달 닫기
// ========================================
function closeProductModal() {
    const modal = document.getElementById('productModal');
    if (modal) {
        modal.style.display = 'none';
        document.body.style.overflow = 'auto';
    }
    currentProductId = null;
}

// ========================================
// 상품 교환
// ========================================
async function exchangeProduct(productId) {
    if (!productId && currentProductId) {
        productId = currentProductId;
    }

    if (!productId) {
        alert('상품을 선택해주세요.');
        return;
    }

    const token = localStorage.getItem('accessToken');

    // 상품 정보 찾기
    const product = currentProducts.find(p => p.productId === productId);
    if (!product) {
        alert('상품 정보를 찾을 수 없습니다.');
        return;
    }

    if (!confirm(`정말 ${product.productName}을(를) ${product.requiredPoint.toLocaleString()}P로 교환하시겠습니까?`)) {
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/points/use`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                amount: product.requiredPoint,
                reason: 'PRODUCT_EXCHANGE',
                pointProductId: productId
            })
        });

        const result = await response.json();

        if (result.success) {
            alert('상품 교환이 완료되었습니다!');
            closeProductModal();
            // 페이지 새로고침
            fetchPointShop(token);
        } else {
            alert(result.message || '상품 교환에 실패했습니다.');
        }
    } catch (error) {
        console.error('상품 교환 오류:', error);
        alert('상품 교환 중 오류가 발생했습니다.');
    }
}