/**
 * 런런 프로젝트 - 크루 목록 화면
 * 무한 스크롤, 검색, 드롭다운 필터 기능 구현
 */

// ===========================
// 전역 변수 및 설정
// ===========================
const API_BASE_URL = '/api/crews';
let currentCursor = null;
const PAGE_SIZE = 5;
let isLoading = false;
let hasMore = true;

let currentFilters = {
    distance: '',
    pace: '',
    recruitStatus: '',
    search: ''
};

const crewListContainer = document.getElementById('crewListContainer');
const loadingSpinner = document.getElementById('loadingSpinner');
const noMoreData = document.getElementById('noMoreData');
const searchInput = document.getElementById('searchInput');
const searchBtn = document.getElementById('searchBtn');

const filterDistance = document.getElementById('filterDistance');
const filterPace = document.getElementById('filterPace');
const filterRecruitStatus = document.getElementById('filterRecruitStatus');

const distanceMenu = document.getElementById('distanceMenu');
const paceMenu = document.getElementById('paceMenu');
const recruitStatusMenu = document.getElementById('recruitStatusMenu');

const distanceLabel = document.getElementById('distanceLabel');
const paceLabel = document.getElementById('paceLabel');
const recruitStatusLabel = document.getElementById('recruitStatusLabel');

// ===========================
// 초기화 함수
// ===========================
document.addEventListener('DOMContentLoaded', () => {
    console.log('크루 목록 페이지 초기화');

    initEventListeners();
    initInfiniteScroll();

    const hasInitialData = crewListContainer.querySelectorAll('.crew-card').length > 0;
    console.log('초기 데이터 존재:', hasInitialData);

    console.log('초기 크루 카드 개수:', crewListContainer.querySelectorAll('.crew-card').length);

    const hasActiveFilters = (currentFilters.search && currentFilters.search.trim()) ||
        (currentFilters.distance && currentFilters.distance.trim()) ||
        (currentFilters.pace && currentFilters.pace.trim()) ||
        (currentFilters.recruitStatus && currentFilters.recruitStatus.trim());

    console.log('초기 필터 상태:', currentFilters);
    console.log('활성 필터 존재:', hasActiveFilters);

    if (hasActiveFilters) {
        console.log('활성 필터 감지 - 서버 데이터 무시하고 API로 재로드');

        const cards = crewListContainer.querySelectorAll('.crew-card');
        cards.forEach(card => card.remove());
        resetAndReload();
    } else if (!hasInitialData) {
        console.log('필터 없음 - 초기 데이터 로드');
        loadMoreCrews();
    } else {
        console.log('서버에서 렌더링된 초기 데이터 있음 - 무한 스크롤만 활성화');

        const lastCard = crewListContainer.querySelector('.crew-card:last-child');
        if (lastCard) {
            const lastCrewId = lastCard.dataset.crewId;
            if (lastCrewId && lastCrewId.trim()) {
                console.log('마지막 크루 ID:', lastCrewId);
                currentCursor = lastCrewId;
            } else {
                console.warn('마지막 카드에 유효한 crew-id가 없음');
            }
        }
    }
});

// ===========================
// 이벤트 리스너 초기화
// ===========================
function initEventListeners() {

    searchBtn.addEventListener('click', handleSearch);

    searchInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            handleSearch();
        }
    });

    filterDistance.addEventListener('click', (e) => {
        e.stopPropagation();
        toggleDropdown(distanceMenu, filterDistance);
    });

    filterPace.addEventListener('click', (e) => {
        e.stopPropagation();
        toggleDropdown(paceMenu, filterPace);
    });

    filterRecruitStatus.addEventListener('click', (e) => {
        e.stopPropagation();
        toggleDropdown(recruitStatusMenu, filterRecruitStatus);
    });

    setupDropdownItems(distanceMenu, 'distance', distanceLabel, filterDistance);
    setupDropdownItems(paceMenu, 'pace', paceLabel, filterPace);
    setupDropdownItems(recruitStatusMenu, 'recruitStatus', recruitStatusLabel, filterRecruitStatus);

    document.addEventListener('click', closeAllDropdowns);
}

// ===========================
// 드롭다운 관련 함수
// ===========================
function toggleDropdown(menu, button) {
    const isOpen = menu.classList.contains('show');

    closeAllDropdowns();

    if (!isOpen) {
        const rect = button.getBoundingClientRect();
        menu.style.position = 'fixed';
        menu.style.top = `${rect.bottom + 4}px`;
        menu.style.left = `${rect.left}px`;
        menu.style.width = `${rect.width}px`;
        menu.classList.add('show');
    }
}

function closeAllDropdowns() {
    document.querySelectorAll('.dropdown-menu').forEach(menu => {
        menu.classList.remove('show');
    });
}

function setupDropdownItems(menu, filterType, label, button) {
    const items = menu.querySelectorAll('.dropdown-item');

    items.forEach(item => {
        item.addEventListener('click', (e) => {
            e.stopPropagation();

            const value = item.dataset.value;
            const text = item.textContent.trim();

            console.log(`필터 선택: ${filterType} = ${value} (${text})`);

            currentFilters[filterType] = value;
            label.textContent = text;

            items.forEach(i => i.classList.remove('active'));
            item.classList.add('active');

            if (value) {
                button.classList.add('btn-filter--active');
                button.setAttribute('data-active', 'true');
            } else {
                button.classList.remove('btn-filter--active');
                button.setAttribute('data-active', 'false');
            }

            menu.classList.remove('show');

            resetAndReload();
        });
    });
}

// ===========================
// 검색 처리
// ===========================
function handleSearch() {
    const newQuery = searchInput.value.trim();

    console.log(`검색 실행: "${newQuery}"`);

    if (newQuery === currentFilters.search) {
        console.log('동일한 검색어');
        return;
    }

    currentFilters.search = newQuery;
    resetAndReload();
}

// ===========================
// 목록 초기화 및 재로딩
// ===========================
function resetAndReload() {
    console.log('목록 초기화 및 재로딩');
    console.log('현재 필터:', currentFilters);

    const cards = crewListContainer.querySelectorAll('.crew-card');
    cards.forEach(card => card.remove());

    const emptyState = crewListContainer.querySelector('.empty-state');
    if (emptyState) emptyState.remove();

    currentCursor = null;
    hasMore = true;

    if (noMoreData) noMoreData.style.display = 'none';

    loadMoreCrews();
}

// ===========================
// 무한 스크롤
// ===========================
function initInfiniteScroll() {
    console.log('무한 스크롤 초기화 (scroll 방식)');

    let scrollTimeout;

    window.addEventListener('scroll', () => {
        clearTimeout(scrollTimeout);

        scrollTimeout = setTimeout(() => {
            const scrollTop = window.scrollY || document.documentElement.scrollTop;
            const scrollHeight = document.documentElement.scrollHeight;
            const clientHeight = window.innerHeight;

            const remaining = scrollHeight - (scrollTop + clientHeight);

            console.log('스크롤 체크:', {
                scrollTop: Math.round(scrollTop),
                scrollHeight,
                clientHeight,
                remaining: Math.round(remaining),
                isLoading,
                hasMore
            });

            // 하단 300px 이내에 도달하면 로딩
            if (remaining < 300 && !isLoading && hasMore) {
                console.log('스크롤 감지 - 추가 데이터 로딩');
                loadMoreCrews();
            }
        }, 100);
    });

    console.log('스크롤 이벤트 등록 완료');
}

// ===========================
// 데이터 로딩
// ===========================
async function loadMoreCrews() {
    if (isLoading || !hasMore) {
        console.log('로딩 중단:', {isLoading, hasMore});
        return;
    }

    isLoading = true;
    showLoading();

    try {
        const url = buildApiUrl();
        console.log('API 요청:', url);

        const token = getAccessToken();
        const headers = {};

        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
            console.log('JWT 토큰 포함됨');
        } else {
            console.log('JWT 토큰 없음 (비로그인 상태)');
        }

        const response = await fetch(url, {
            method: 'GET',
            headers: headers
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        const apiResponse = await response.json();
        console.log('API 응답:', apiResponse);

        const page = apiResponse.data || apiResponse;

        if (!page?.crews || !Array.isArray(page.crews)) {
            console.warn('유효하지 않은 응답 구조');
            hasMore = false;
            hideLoading();
            showNoMoreData();
            return;
        }

        const hasPaceFilter = currentFilters.pace && currentFilters.pace.trim();
        const hasDistanceFilter = currentFilters.distance && currentFilters.distance.trim();

        const hasClientSideFilter = hasPaceFilter || hasDistanceFilter;


        console.log(`${page.crews.length}개 크루 렌더링`);
        const renderedCount = renderCrews(page.crews);

        if (hasClientSideFilter && page.crews.length === 0) {
            console.log('서버 응답 없음 - 클라이언트 필터 적용 불가');
            hasMore = false;
            hideLoading();

            const existingCards = crewListContainer.querySelectorAll('.crew-card');
            if (existingCards.length === 0) {
                showEmptyMessage();
            } else {
                showNoMoreData();
            }
            return;
        }

        if (!hasClientSideFilter && page.crews.length === 0) {
            console.log('검색 결과 없음');
            hasMore = false;
            hideLoading();

            if (!currentCursor) {
                showEmptyMessage();
            } else {
                showNoMoreData();
            }
            return;
        }

        if (hasClientSideFilter && renderedCount === 0) {
            console.log(`클라이언트 필터 적용 후 이번 페이지 결과 0개 (${page.crews.length}개 중 0개 매칭)`);
        }

        const pagination = {
            hasNext: page.hasMore || false,
            nextCursor: page.nextCursor || null
        };

        console.log('페이지네이션:', pagination);

        if (pagination.nextCursor) {
            updatePagination(pagination);
        } else {

            console.log('페이지네이션 정보 없음 - 마지막 페이지');
            hasMore = false;
            showNoMoreData();

        }

    } catch (error) {
        console.error('로드 실패:', error);
        alert('크루 목록을 불러오지 못했습니다.\n' + error.message);
    } finally {
        isLoading = false;
        hideLoading();
    }
}

// ===========================
// API URL 생성 (필터 포함)
// ===========================
function buildApiUrl() {
    const params = new URLSearchParams();

    if (currentCursor) {
        params.append('cursor', currentCursor);
    }

    if (currentFilters.search && currentFilters.search.trim()) {
        params.append('keyword', currentFilters.search.trim());
    }

    if (currentFilters.recruitStatus === 'recruiting') {

        params.append('recruiting', 'true');
    } else if (currentFilters.recruitStatus === 'closed') {

        params.append('recruiting', 'false');
    }

    params.append('size', PAGE_SIZE);

    const url = `${API_BASE_URL}?${params.toString()}`;
    console.log('생성된 API URL:', url);
    console.log('적용된 필터:', {
        search: currentFilters.search || '(없음)',
        distance: currentFilters.distance || '(없음)',
        pace: currentFilters.pace || '(없음)',
        recruitStatus: currentFilters.recruitStatus || '(없음)'
    });
    return url;
}

// ===========================
// 렌더링
// ===========================
function renderCrews(crews) {
    console.log(`renderCrews 시작: ${crews.length}개 크루`);
    let filteredCrews = crews;

    if (currentFilters.distance && currentFilters.distance.trim()) {
        const filter = currentFilters.distance.trim();
        const beforeCount = filteredCrews.length;

        if (filter !== '') {
            filteredCrews = filteredCrews.filter(crew => {
                if (!crew.distance) return false;

                return crew.distance.trim() === filter;
            });
        }

        console.log(`거리 필터 적용 (${filter || '전체'}): ${beforeCount}개 → ${filteredCrews.length}개`);
    }


    if (currentFilters.pace && currentFilters.pace.trim()) {
        const paceFilter = currentFilters.pace.trim();
        const beforeCount = filteredCrews.length;

        if (paceFilter !== '') {
            filteredCrews = filteredCrews.filter(crew => {
                if (!crew.averagePace) return false;

                return crew.averagePace.trim() === paceFilter;
            });
        }

        console.log(`페이스 필터 적용 (${paceFilter || '전체'}): ${beforeCount}개 → ${filteredCrews.length}개`);
    }


    const renderedCards = [];

    filteredCrews.forEach(crew => {
        const card = createCrewCard(crew);
        renderedCards.push({crew, card});
        crewListContainer.appendChild(card);
    });

    // 로그인한 경우에만 PENDING 배지 표시 시도
    const token = getAccessToken();
    if (token && renderedCards.length > 0) {
        annotatePendingBadges(renderedCards.map(rc => rc.crew.crewId), token);
    }

    return filteredCrews.length;
}


/**
 * 페이스 문자열을 분 단위 숫자로 변환
 */
function parsePaceToMinutes(paceStr) {
    if (!paceStr) return null;

    const colonMatch = paceStr.match(/(\d+):(\d+)/);
    if (colonMatch) {
        const minutes = parseInt(colonMatch[1], 10);
        const seconds = parseInt(colonMatch[2], 10);
        return minutes + (seconds / 60);
    }

    const minuteMatch = paceStr.match(/(\d+(?:\.\d+)?)분/);
    if (minuteMatch) {
        return parseFloat(minuteMatch[1]);
    }

    const numberMatch = paceStr.match(/^(\d+(?:\.\d+)?)$/);
    if (numberMatch) {
        return parseFloat(numberMatch[1]);
    }

    return null;
}

function createCrewCard(crew) {
    const article = document.createElement('article');
    article.className = 'crew-card';
    article.setAttribute('data-crew-id', crew.crewId);
    article.setAttribute('role', 'button');
    article.setAttribute('tabindex', '0');

    article.addEventListener('click', () => {
        window.location.href = `/crews/${crew.crewId}`;
    });

    article.addEventListener('keypress', (e) => {
        if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            window.location.href = `/crews/${crew.crewId}`;
        }
    });

    const badge = (crew.crewRecruitStatus === 'RECRUITING' || crew.crewRecruitStatus === 'OPEN')
        ? '<span class="crew-card__badge crew-card__badge--recruiting">모집중</span>'
        : '<span class="crew-card__badge crew-card__badge--closed">모집마감</span>';

    const distance = crew.distance
        ? `<span class="running-info__item">
             <span class="running-icon">🏃</span>
             <span class="running-info__label">거리:</span>
             <span class="running-info__value">${escapeHtml(crew.distance)}</span>
           </span>`
        : '';

    const pace = crew.averagePace
        ? `<span class="running-info__item">
             <span class="running-icon">⏱️</span>
             <span class="running-info__label">페이스:</span>
             <span class="running-info__value">${escapeHtml(crew.averagePace)}</span>
           </span>`
        : '<span class="running-info__item"><span class="running-icon">⏱️</span><span class="running-info__label">페이스:</span><span class="running-info__value">-</span></span>';

    const defaultImageUrl = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0Y1RjVGNSIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBmb250LXNpemU9IjE4IiBmaWxsPSIjOTk5IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBkeT0iLjNlbSI+6rCA7J2AIOyVlOydgCDsiqTsmYg8L3RleHQ+PC9zdmc+';
    const imageUrl = crew.crewImageUrl && crew.crewImageUrl.trim()
        ? crew.crewImageUrl
        : defaultImageUrl;

    article.innerHTML = `
        <div class="crew-card__image-wrapper">
            <img src="${imageUrl}" 
                 alt="${escapeHtml(crew.crewName)}"
                 class="crew-card__image" 
                 loading="lazy"
                 onerror="this.style.backgroundColor='#F5F5F5'; this.style.display='flex'; this.style.alignItems='center'; this.style.justifyContent='center'; this.outerHTML='<div class=\\'crew-card__image-wrapper\\' style=\\'background-color:#F5F5F5;display:flex;align-items:center;justify-content:center;min-height:120px;color:#999;\\'>이미지 없음</div>'">
        </div>
        <div class="crew-card__content">
            <div class="crew-card__header">
                <h2 class="crew-card__title">${escapeHtml(crew.crewName)}</h2>
                ${badge}
            </div>
            <div class="crew-card__details">
                <div class="crew-card__detail-item">
                    <span class="detail-icon">📍</span>
                    <span>${escapeHtml(crew.region || '위치 미정')}</span>
                </div>
                <div class="crew-card__detail-item">
                    <span class="detail-icon">👥</span>
                    <span>${crew.memberCount || 0}명 참여중</span>
                </div>
            </div>
            <div class="crew-card__running-info">
                ${distance}
                ${pace}
            </div>
        </div>
    `;

    return article;
}

// ===========================
// 페이지네이션 업데이트
// ===========================
function updatePagination(pagination) {
    if (!pagination) {
        console.log('페이지네이션 정보 없음');
        hasMore = false;
        showNoMoreData();
        return;
    }

    console.log('페이지네이션:', pagination);

    currentCursor = pagination.nextCursor;

    if (!pagination.hasNext || !currentCursor) {
        console.log('마지막 페이지 도달');
        hasMore = false;
        showNoMoreData();
    } else {
        console.log('다음 페이지 존재, cursor:', currentCursor);
    }
}

// ===========================
// UI 상태 관리
// ===========================
function showLoading() {
    if (loadingSpinner) {
        loadingSpinner.style.display = 'flex';
    }
}

function hideLoading() {
    if (loadingSpinner) {
        loadingSpinner.style.display = 'none';
    }
}

function showNoMoreData() {
    if (noMoreData && !hasMore) {
        noMoreData.style.display = 'flex';
    }
}

function showEmptyMessage() {
    const empty = document.createElement('div');
    empty.className = 'empty-state';
    empty.innerHTML = `
        <p style="font-size: 16px; color: #666; text-align: center; margin-top: 60px;">
            검색 결과가 없습니다
        </p>
        <p style="font-size: 14px; color: #999; text-align: center; margin-top: 8px;">
            다른 조건으로 검색해보세요
        </p>
    `;
    crewListContainer.appendChild(empty);
}

// ===========================
// 유틸리티 함수
// ===========================
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

/**
 * 특정 크루 카드에 상태 배지 표시
 */
function setStatusBadge(crewId, state) {
    const card = crewListContainer.querySelector(`.crew-card[data-crew-id="${crewId}"]`);
    if (!card) return;

    const header = card.querySelector('.crew-card__header');
    if (!header) return;

    // 기존 상태 배지 제거
    const existingBadge = header.querySelector('[data-status-badge]');
    if (existingBadge) {
        existingBadge.remove();
    }

    // 상태에 따라 배지 생성
    let badge = null;

    if (state === 'APPROVED') {
        badge = document.createElement('span');
        badge.className = 'crew-card__badge crew-card__badge--approved';
        badge.setAttribute('data-status-badge', 'approved');
        badge.innerHTML = '<span aria-hidden="true">🔴</span>참여중';
    } else if (state === 'PENDING') {
        badge = document.createElement('span');
        badge.className = 'crew-card__badge crew-card__badge--pending';
        badge.setAttribute('data-status-badge', 'pending');
        badge.innerHTML = '<span aria-hidden="true">🔵</span>요청중';
    }

    if (badge) {
        header.appendChild(badge);
    }
}

/**
 * 현재 사용자 기준 상태에 따라 배지 표시
 */
async function annotatePendingBadges(crewIds, token) {
    if (!crewIds || crewIds.length === 0) return;
    const headers = {'Authorization': `Bearer ${token}`};

    await Promise.allSettled(crewIds.map(async (id) => {
        try {
            const res = await fetch(`/api/crews/${id}/applied`, {method: 'GET', headers});
            if (!res.ok) return;
            const json = await res.json().catch(() => null);
            const data = json?.data || json;
            const state = data?.crewJoinState || data?.state || data?.joinStatus;

            // APPROVED 우선, 그 다음 PENDING
            if (state === 'APPROVED' || state === 'PENDING') {
                setStatusBadge(id, state);
            }
        } catch (e) {
            console.warn('배지 조회 실패', id, e);
        }
    }));
}

/**
 * 액세스 토큰 가져오기
 */
function getAccessToken() {
    try {
        const token = localStorage.getItem('accessToken');
        return token;
    } catch (error) {
        console.warn('토큰 가져오기 실패:', error);
        return null;
    }
}

// ===========================
// 스크립트 로드 완료
// ===========================
console.log('크루 목록 스크립트 로드 완료');
console.log('현재 필터 상태:', currentFilters);