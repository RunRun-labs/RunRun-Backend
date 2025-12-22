/**
 * 런런 프로젝트 - 크루 목록 화면
 * 무한 스크롤, 검색, 드롭다운 필터 기능 구현
 */

// ===========================
// 전역 변수 및 설정
// ===========================
const API_BASE_URL = '/api/crews';
let currentCursor = null;
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
    const observer = new IntersectionObserver((entries) => {
        if (entries[0].isIntersecting && !isLoading && hasMore) {
            console.log('스크롤 감지 - 추가 데이터 로딩');
            loadMoreCrews();
        }
    }, {
        rootMargin: '200px'
    });

    if (loadingSpinner) {
        observer.observe(loadingSpinner);
    }
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
        const hasDistanceRangeFilter = currentFilters.distance &&
            (currentFilters.distance.includes('미만') || currentFilters.distance.includes('이상'));

        const hasClientSideFilter = hasPaceFilter || hasDistanceRangeFilter;

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

        if (page.pagination) {
            updatePagination(page.pagination);
        } else {

            console.log('페이지네이션 정보 없음 - 마지막 페이지');
            hasMore = false;

            if (hasClientSideFilter && renderedCount === 0) {
                const existingCards = crewListContainer.querySelectorAll('.crew-card');
                if (existingCards.length === 0) {
                    showEmptyMessage();
                } else {
                    showNoMoreData();
                }
            } else {
                showNoMoreData();
            }
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

    if (currentFilters.distance && currentFilters.distance.trim()) {
        const value = currentFilters.distance.trim();

        if (!value.includes('미만') && !value.includes('이상')) {
            const num = parseInt(value.replace(/[^0-9]/g, ''), 10);
            if (!isNaN(num)) {
                params.append('distance', `${num}km`);
            }
        }
    }

    if (currentFilters.recruitStatus === 'recruiting') {

        params.append('recruiting', 'true');
    } else if (currentFilters.recruitStatus === 'closed') {

        params.append('recruiting', 'false');
    }

    params.append('size', '10');

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
        const filterNum = parseInt(filter.replace(/[^0-9]/g, ''), 10);

        console.log(`거리 필터 적용 시작: "${filter}", 숫자: ${filterNum}`);

        if (!isNaN(filterNum)) {
            const beforeCount = filteredCrews.length;
            filteredCrews = filteredCrews.filter(crew => {
                if (!crew.distance) {
                    console.log(`크루 ${crew.crewId || crew.crewName}: 거리 정보 없음`);
                    return false;
                }

                const crewNum = parseInt(
                    crew.distance.replace(/[^0-9]/g, ''),
                    10
                );

                if (isNaN(crewNum)) {
                    console.log(`크루 ${crew.crewId || crew.crewName}: 거리 파싱 실패 (${crew.distance})`);
                    return false;
                }

                let matches = false;
                if (filter.includes('미만')) {
                    matches = crewNum < filterNum;
                } else if (filter.includes('이상')) {
                    matches = crewNum >= filterNum;
                } else {

                    matches = crewNum === filterNum;
                }

                if (matches) {
                    console.log(`크루 ${crew.crewId || crew.crewName}: ${crewNum}km 매칭 (필터: ${filter})`);
                }
                return matches;
            });

            console.log(`거리 필터 적용 완료: ${beforeCount}개 → ${filteredCrews.length}개 (${filter})`);
        }
    }

    if (currentFilters.pace && currentFilters.pace.trim()) {
        const paceFilter = currentFilters.pace.trim();

        filteredCrews = filteredCrews.filter(crew => {
            if (!crew.averagePace) return false;

            const pace = crew.averagePace.trim();
            const paceMinutes = parsePaceToMinutes(pace);

            if (paceMinutes === null) return false;

            if (paceFilter.includes('이하')) {

                const maxPace = parseFloat(paceFilter.match(/(\d+(?:\.\d+)?)/)?.[1]);
                if (!isNaN(maxPace)) {
                    return paceMinutes <= maxPace;
                }
                return false;
            } else if (paceFilter.includes('이상')) {

                const minPace = parseFloat(paceFilter.match(/(\d+(?:\.\d+)?)/)?.[1]);
                if (!isNaN(minPace)) {
                    return paceMinutes >= minPace;
                }
                return false;
            } else if (paceFilter.includes('~') || paceFilter.includes('-')) {

                const rangeMatch = paceFilter.match(/(\d+(?:\.\d+)?)[~-](\d+(?:\.\d+)?)/);
                if (rangeMatch) {
                    const minPace = parseFloat(rangeMatch[1]);
                    const maxPace = parseFloat(rangeMatch[2]);
                    if (!isNaN(minPace) && !isNaN(maxPace)) {
                        return paceMinutes >= minPace && paceMinutes <= maxPace;
                    }
                }
                return false;
            }

            return false;
        });

        console.log(`페이스 필터 적용 (${paceFilter}): ${crews.length}개 → ${filteredCrews.length}개`);
    }

    filteredCrews.forEach(crew => {
        const card = createCrewCard(crew);
        crewListContainer.appendChild(card);
    });

    return filteredCrews.length;
}


/**
 * 페이스 문자열을 분 단위 숫자로 변환
 * 지원 형식: "3:00/km", "5분/km", "5~6분/km", "3"
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
             <span class="running-info__label">거리:</span>
             <span class="running-info__value">${escapeHtml(crew.distance)}</span>
           </span>`
        : '';

    const pace = crew.averagePace
        ? `<span class="running-info__item">
             <span class="running-info__label">페이스:</span>
             <span class="running-info__value">${escapeHtml(crew.averagePace)}</span>
           </span>`
        : '<span class="running-info__item"><span class="running-info__label">페이스:</span><span class="running-info__value">-</span></span>';

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
                    <svg class="detail-icon" width="16" height="16" viewBox="0 0 16 16" fill="none">
                        <path d="M8 1C5.23858 1 3 3.23858 3 6C3 9.25 8 15 8 15C8 15 13 9.25 13 6C13 3.23858 10.7614 1 8 1Z" stroke="currentColor" stroke-width="1.5"/>
                        <circle cx="8" cy="6" r="1.5" fill="currentColor"/>
                    </svg>
                    <span>${escapeHtml(crew.region || '위치 미정')}</span>
                </div>
                <div class="crew-card__detail-item">
                    <svg class="detail-icon" width="16" height="16" viewBox="0 0 16 16" fill="none">
                        <path d="M8 8C9.65685 8 11 6.65685 11 5C11 3.34315 9.65685 2 8 2C6.34315 2 5 3.34315 5 5C5 6.65685 6.34315 8 8 8Z" stroke="currentColor" stroke-width="1.5"/>
                        <path d="M2 14C2 11.2386 4.23858 9 7 9H9C11.7614 9 14 11.2386 14 14" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                    </svg>
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