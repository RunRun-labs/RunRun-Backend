// 전역 변수
let currentPage = 0;
let totalPages = 0;
let currentFilter = {
    isAvailable: '',
    keyword: ''
};
let selectedProducts = new Set();
let isEditMode = false;

// 토큰 가져오기 (localStorage에서)
function getAuthToken() {
    return localStorage.getItem('accessToken');
}

// API 헤더 생성
function getHeaders() {
    const token = getAuthToken();
    return {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
    };
}

// 페이지 로드 시 실행
document.addEventListener('DOMContentLoaded', function () {
    initializeEventListeners();
    loadProducts();
});

function setFieldError(inputEl, errorElId, message) {
    const errorEl = document.getElementById(errorElId);

    if (message) {
        inputEl.classList.add('input-error');
        if (errorEl) {
            errorEl.textContent = message;
            errorEl.classList.add('active');
        }
        return false;
    } else {
        inputEl.classList.remove('input-error');
        if (errorEl) {
            errorEl.textContent = '';
            errorEl.classList.remove('active');
        }
        return true;
    }
}

function setImageError(message) {
    const preview = document.getElementById('imagePreview');
    const errorEl = document.getElementById('errProductImage');

    if (message) {
        preview.classList.add('input-error');
        errorEl.textContent = message;
        errorEl.classList.add('active');
        return false;
    } else {
        preview.classList.remove('input-error');
        errorEl.textContent = '';
        errorEl.classList.remove('active');
        return true;
    }
}

function validateProductName() {
    const el = document.getElementById('productName');
    const v = el.value.trim();
    if (!v) return setFieldError(el, 'errProductName', '필수 입력 항목입니다.');
    if (v.length < 2) return setFieldError(el, 'errProductName', '상품명은 2자 이상 입력해주세요.');
    if (v.length > 100) return setFieldError(el, 'errProductName', '상품명은 100자 이하여야 합니다.');
    return setFieldError(el, 'errProductName', null);
}

function validateProductDescription() {
    const el = document.getElementById('productDescription');
    const v = el.value.trim();
    if (!v) return setFieldError(el, 'errProductDescription', '필수 입력 항목입니다.');
    if (v.length < 10) return setFieldError(el, 'errProductDescription', '상품 설명은 10자 이상이어야 합니다.');
    if (v.length > 1000) return setFieldError(el, 'errProductDescription', '상품 설명은 1000자 이하여야 합니다.');
    return setFieldError(el, 'errProductDescription', null);
}

function validateRequiredPoint() {
    const el = document.getElementById('requiredPoint');
    const raw = el.value;
    if (raw === '' || raw === null) return setFieldError(el, 'errRequiredPoint', '필수 입력 항목입니다.');

    const v = Number(raw);
    if (!Number.isFinite(v)) return setFieldError(el, 'errRequiredPoint', '숫자만 입력 가능합니다.');
    if (v < 1) return setFieldError(el, 'errRequiredPoint', '필요 포인트는 1 이상이어야 합니다.');
    return setFieldError(el, 'errRequiredPoint', null);
}

function validateImageRequired() {
    const key = document.getElementById('productImageUrl').value;
    if (!key) return setImageError('상품 이미지는 필수입니다.');
    return setImageError(null);
}

function validateAllFields() {
    const a = validateProductName();
    const b = validateProductDescription();
    const c = validateRequiredPoint();
    const d = validateImageRequired();
    return a && b && c && d;
}

function clearValidationErrors() {
    // inputs
    setFieldError(document.getElementById('productName'), 'errProductName', null);
    setFieldError(document.getElementById('productDescription'), 'errProductDescription', null);
    setFieldError(document.getElementById('requiredPoint'), 'errRequiredPoint', null);
    setImageError(null);
}

// 폼 필드 순서(이미지는 별도)
const FORM_FIELDS = ['productName', 'productDescription', 'requiredPoint'];

function validateUpTo(fieldId) {
    // fieldId 이전(포함 X)까지 모두 검증
    const idx = FORM_FIELDS.indexOf(fieldId);
    if (idx <= 0) return;

    for (let i = 0; i < idx; i++) {
        const id = FORM_FIELDS[i];
        if (id === 'productName') validateProductName();
        if (id === 'productDescription') validateProductDescription();
        if (id === 'requiredPoint') validateRequiredPoint();
    }
}

function validateAllBeforeImage() {
    // 이미지 클릭 시: 텍스트/숫자 필드 전부 + 이미지 필수까지
    validateProductName();
    validateProductDescription();
    validateRequiredPoint();
    validateImageRequired();
}

// 이벤트 리스너 초기화
function initializeEventListeners() {
    // 검색
    document.getElementById('searchInput').addEventListener('input', debounce(function (e) {
        currentFilter.keyword = e.target.value;
        currentPage = 0;
        loadProducts();
    }, 500));

    // 필터
    document.getElementById('filterAvailable').addEventListener('change', function (e) {
        currentFilter.isAvailable = e.target.value;
        currentPage = 0;
        loadProducts();
    });

    // 생성 버튼
    document.getElementById('btnCreate').addEventListener('click', openCreateModal);

    // 삭제 버튼
    document.getElementById('btnDelete').addEventListener('click', deleteSelectedProducts);

    // 모달 닫기
    document.getElementById('closeModal').addEventListener('click', closeModal);
    document.getElementById('btnCancel').addEventListener('click', closeModal);

    // 전체 선택
    document.getElementById('selectAll').addEventListener('change', toggleSelectAll);

    // 폼 제출
    document.getElementById('productForm').addEventListener('submit', handleFormSubmit);

    document.getElementById('productImage').addEventListener('change', handleImageUpload);

    // 모달 외부 클릭 시 닫기
    window.addEventListener('click', function (e) {
        const modal = document.getElementById('productModal');
        if (e.target === modal) {
            closeModal();
        }
    });

    // ===== Validation: 개별 blur =====
    document.getElementById('productName').addEventListener('blur', validateProductName);
    document.getElementById('productDescription').addEventListener('blur', validateProductDescription);
    document.getElementById('requiredPoint').addEventListener('blur', validateRequiredPoint);

// ===== Validation: 다음 필드 클릭/포커스 시 이전 필드 강제 검증 =====

// 상품 설명 클릭/포커스 시 → 상품명 검증
    document.getElementById('productDescription').addEventListener('focus', function () {
        validateUpTo('productDescription'); // productName
    });

// 필요 포인트 클릭/포커스 시 → 상품명 + 상품설명 검증
    document.getElementById('requiredPoint').addEventListener('focus', function () {
        validateUpTo('requiredPoint'); // productName, productDescription
    });

    // 이미지
    document.getElementById('imagePreview').addEventListener('click', function () {
        validateAllBeforeImage();
        // 그 다음 파일 선택 열기 (기존 로직 유지)
        document.getElementById('productImage').click();
    });
}

// 상품 목록 로드
async function loadProducts() {
    try {
        const params = new URLSearchParams({
            page: currentPage,
            size: 20,
            sort: 'createdAt,desc'
        });

        if (currentFilter.isAvailable) {
            params.append('isAvailable', currentFilter.isAvailable);
        }

        if (currentFilter.keyword) {
            params.append('keyword', currentFilter.keyword);
        }

        const response = await fetch(`/api/admin/points/products?${params}`, {
            headers: getHeaders()
        });

        if (!response.ok) throw new Error('상품 목록 조회 실패');

        const result = await response.json();
        const data = result.data;

        renderProductTable(data.items);
        renderPagination(data.page, data.totalPages);
        totalPages = data.totalPages;

    } catch (error) {
        console.error('Error:', error);
        alert('상품 목록을 불러오는데 실패했습니다.');
    }
}

// 테이블 렌더링
function renderProductTable(products) {
    const tbody = document.getElementById('productTableBody');

    if (!products || products.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="9" style="text-align: center; padding: 40px;">
                    등록된 상품이 없습니다.
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = products.map((product, index) => {
        // S3 key를 HTTPS URL로 변환
        const imageUrl = product.productImageUrl && product.productImageUrl.startsWith('http')
            ? product.productImageUrl
            : product.productImageUrl
                ? `https://runrun-uploads-bucket.s3.mx-central-1.amazonaws.com/${product.productImageUrl}`
                : '/images/no-image.png';  // 이미지 없을 때 기본 이미지

        return `
        <tr>
            <td><input type="checkbox" class="product-checkbox" data-id="${product.productId}"></td>
            <td>${currentPage * 20 + index + 1}</td>
            <td>
                <img src="${imageUrl}" 
                     alt="${product.productName}" class="product-image">
            </td>
            <td>${product.productName}</td>
            <td>${product.requiredPoint.toLocaleString()} P</td>
            <td>
                <span class="status-badge ${product.isAvailable ? 'status-active' : 'status-inactive'}">
                    ${product.isAvailable ? '판매중' : '판매중지'}
                </span>
            </td>
            <td>${formatDate(product.createdAt)}</td>
            <td>${formatDate(product.updatedAt)}</td>
            <td>
                <div class="action-buttons">
                    <button class="btn-edit" onclick="openEditModal(${product.productId})">수정</button>
                </div>
            </td>
        </tr>
    `
    }).join('');

    // 체크박스 이벤트 리스너
    document.querySelectorAll('.product-checkbox').forEach(checkbox => {
        checkbox.addEventListener('change', function () {
            const id = parseInt(this.dataset.id);
            if (this.checked) {
                selectedProducts.add(id);
            } else {
                selectedProducts.delete(id);
            }
        });
    });
}

// 페이지네이션 렌더링
function renderPagination(currentPageNum, totalPagesNum) {
    const pagination = document.getElementById('pagination');

    if (totalPagesNum <= 1) {
        pagination.innerHTML = '';
        return;
    }

    let html = '';

    // 이전 버튼
    if (currentPageNum > 0) {
        html += `<button class="page-btn" onclick="changePage(${currentPageNum - 1})">이전</button>`;
    }

    // 페이지 번호
    const startPage = Math.max(0, currentPageNum - 2);
    const endPage = Math.min(totalPagesNum - 1, currentPageNum + 2);

    for (let i = startPage; i <= endPage; i++) {
        html += `<button class="page-btn ${i === currentPageNum ? 'active' : ''}" 
                        onclick="changePage(${i})">${i + 1}</button>`;
    }

    // 다음 버튼
    if (currentPageNum < totalPagesNum - 1) {
        html += `<button class="page-btn" onclick="changePage(${currentPageNum + 1})">다음</button>`;
    }

    pagination.innerHTML = html;
}

// 페이지 변경
function changePage(page) {
    currentPage = page;
    loadProducts();
    window.scrollTo({top: 0, behavior: 'smooth'});
}

// 생성 모달 열기
function openCreateModal() {
    isEditMode = false;
    document.getElementById('modalTitle').textContent = '포인트 상품 생성';
    document.getElementById('productForm').reset();
    document.getElementById('productId').value = '';
    document.getElementById('productImageUrl').value = '';
    document.getElementById('previewImg').style.display = 'none';
    document.getElementById('uploadPlaceholder').style.display = 'block';
    clearValidationErrors();
    document.getElementById('productModal').style.display = 'block';
}

// 수정 모달 열기
async function openEditModal(productId) {
    try {
        // 상품 상세 정보 조회
        const params = new URLSearchParams({
            page: 0,
            size: 1000
        });

        const response = await fetch(`/api/admin/points/products?${params}`, {
            headers: getHeaders()
        });

        if (!response.ok) throw new Error('상품 조회 실패');

        const result = await response.json();
        const product = result.data.items.find(p => p.productId === productId);

        if (!product) throw new Error('상품을 찾을 수 없습니다');

        isEditMode = true;
        document.getElementById('modalTitle').textContent = '포인트 상품 수정';
        document.getElementById('productId').value = product.productId;
        document.getElementById('productName').value = product.productName;
        document.getElementById('productDescription').value = product.productDescription;
        document.getElementById('requiredPoint').value = product.requiredPoint;
        document.getElementById('isAvailable').checked = product.isAvailable;
        document.getElementById('productImageUrl').value = product.productImageUrl;

        // 이미지 미리보기
        if (product.productImageUrl) {
            const previewImg = document.getElementById('previewImg');

            // S3 key를 HTTPS URL로 변환
            const imageUrl = product.productImageUrl.startsWith('http')
                ? product.productImageUrl
                : `https://runrun-uploads-bucket.s3.mx-central-1.amazonaws.com/${product.productImageUrl}`;

            previewImg.src = imageUrl;
            previewImg.style.display = 'block';
            document.getElementById('uploadPlaceholder').style.display = 'none';
        }

        document.getElementById('productModal').style.display = 'block';

    } catch (error) {
        console.error('Error:', error);
        alert('상품 정보를 불러오는데 실패했습니다.');
    }
}

// 모달 닫기
function closeModal() {
    document.getElementById('productModal').style.display = 'none';
    document.getElementById('productForm').reset();
    clearValidationErrors();
}

// 폼 제출
async function handleFormSubmit(e) {
    e.preventDefault();

    const ok = validateAllFields();
    if (!ok) return;

    const productName = document.getElementById('productName').value.trim();
    const productDescription = document.getElementById('productDescription').value.trim();
    const requiredPoint = parseInt(document.getElementById('requiredPoint').value, 10);
    const productImageUrl = document.getElementById('productImageUrl').value;
    const isAvailable = document.getElementById('isAvailable').checked;

    const requestData = {
        productName,
        productDescription,
        requiredPoint,
        productImageUrl,
        isAvailable
    };

    try {
        let url, method;

        if (isEditMode) {
            const productId = document.getElementById('productId').value;
            url = `/api/admin/points/products/${productId}`;
            method = 'PUT';
        } else {
            url = '/api/admin/points/products';
            method = 'POST';
        }

        const response = await fetch(url, {
            method: method,
            headers: getHeaders(),
            body: JSON.stringify(requestData)
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || '저장 실패');
        }

        alert(isEditMode ? '상품이 수정되었습니다.' : '상품이 생성되었습니다.');
        closeModal();
        loadProducts();

    } catch (error) {
        console.error('Error:', error);
        alert(error.message);
    }
}

// 이미지 업로드 (S3)
async function handleImageUpload(e) {
    const file = e.target.files[0];
    if (!file) return;

    // 파일 크기 체크 (5MB)
    if (file.size > 5 * 1024 * 1024) {
        alert('파일 크기는 5MB 이하여야 합니다.');
        return;
    }

    // 이미지 파일 체크
    if (!file.type.startsWith('image/')) {
        alert('이미지 파일만 업로드 가능합니다.');
        return;
    }

    // 검증 통과 시 에러 제거
    setImageError(null);

    try {
        const formData = new FormData();
        formData.append('file', file);

        const response = await fetch('/api/admin/points/products/upload', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${getAuthToken()}`
            },
            body: formData
        });

        if (!response.ok) throw new Error('이미지 업로드 실패');

        const result = await response.json();
        const imageKey = result.data.key;

        // hidden input에 저장
        document.getElementById('productImageUrl').value = imageKey;

        validateImageRequired();

        // 미리보기 표시
        const reader = new FileReader();
        reader.onload = function (e) {
            const previewImg = document.getElementById('previewImg');
            previewImg.src = e.target.result;
            previewImg.style.display = 'block';
            document.getElementById('uploadPlaceholder').style.display = 'none';
        };
        reader.readAsDataURL(file);

    } catch (error) {
        console.error('Error:', error);
        alert('이미지 업로드에 실패했습니다.');
    }
}

// 선택된 상품 삭제
async function deleteSelectedProducts() {
    if (selectedProducts.size === 0) {
        alert('삭제할 상품을 선택해주세요.');
        return;
    }

    if (!confirm(`선택한 ${selectedProducts.size}개의 상품을 삭제하시겠습니까?`)) {
        return;
    }

    try {
        const deletePromises = Array.from(selectedProducts).map(productId =>
            fetch(`/api/admin/points/products/${productId}`, {
                method: 'DELETE',
                headers: getHeaders()
            })
        );

        await Promise.all(deletePromises);

        alert('삭제되었습니다.');
        selectedProducts.clear();
        document.getElementById('selectAll').checked = false;
        loadProducts();

    } catch (error) {
        console.error('Error:', error);
        alert('삭제 중 오류가 발생했습니다.');
    }
}

// 전체 선택/해제
function toggleSelectAll(e) {
    const checkboxes = document.querySelectorAll('.product-checkbox');
    checkboxes.forEach(checkbox => {
        checkbox.checked = e.target.checked;
        const id = parseInt(checkbox.dataset.id);
        if (e.target.checked) {
            selectedProducts.add(id);
        } else {
            selectedProducts.delete(id);
        }
    });
}

// 날짜 포맷
function formatDate(dateString) {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleDateString('ko-KR') + ' ' + date.toLocaleTimeString('ko-KR', {
        hour: '2-digit',
        minute: '2-digit'
    });
}

// 디바운스 유틸
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}