document.addEventListener("DOMContentLoaded", () => {
    // HTML 내 스크립트에서 설정한 typeParam 사용
    if (typeof typeParam !== 'undefined') {
        loadTerms(typeParam);
    } else {
        console.error("약관 타입을 찾을 수 없습니다.");
    }
    
    // 뒤로가기 버튼 핸들러
    attachBackButtonHandler();
    
    // 헤더 제목 업데이트
    updateHeaderTitle();
});

// 뒤로가기 버튼 핸들러
function attachBackButtonHandler() {
    const backButton = document.querySelector(".back-button");
    if (backButton) {
        backButton.addEventListener("click", () => {
            // 설정 페이지로 이동하면서 약관 모달 열기 상태로 전달
            window.location.href = "/setting?openTermsModal=true";
        });
    }
}

// 헤더 제목 업데이트
function updateHeaderTitle() {
    const headerTitle = document.getElementById("headerTitle");
    if (headerTitle && typeof typeParam !== 'undefined') {
        const titleMap = {
            'SERVICE': '서비스 이용약관',
            'PRIVACY': '개인정보 처리방침',
            'MARKETING': '마케팅 정보'
        };
        headerTitle.textContent = titleMap[typeParam] || '약관';
    }
}

async function loadTerms(type) {
    const contentEl = document.getElementById("termsContent");
    const titleEl = document.getElementById("displayTitle");
    const versionEl = document.getElementById("displayVersion");
    const dateEl = document.getElementById("displayDate");

    try {
        // API 호출 (GET /terms/{type})
        const response = await fetch(`/terms/${type}`, {
            method: "GET",
            headers: {
                // 토큰이 없어도 조회 가능하도록 SecurityConfig에서 허용 필요
                // "Content-Type": "application/json"
            }
        });

        if (!response.ok) {
            throw new Error("약관 정보를 불러오지 못했습니다.");
        }

        const payload = await response.json();
        const data = payload.data; // TermsResDto

        // 1. 메타 정보 바인딩
        titleEl.textContent = data.title;
        versionEl.textContent = `Ver. ${data.version}`;
        if (data.createdAt) {
            const date = new Date(data.createdAt);
            dateEl.textContent = ` | 시행일: ${date.getFullYear()}.${date.getMonth() + 1}.${date.getDate()}`;
        }

        // 2. 마크다운 변환 및 렌더링 (핵심)
        // marked 라이브러리가 마크다운 문자열을 HTML 문자열로 변환해줍니다.
        // 예: "# 제목" -> "<h1>제목</h1>"
        const htmlContent = marked.parse(data.content);

        // 3. HTML 주입
        contentEl.innerHTML = htmlContent;

    } catch (error) {
        console.error(error);
        contentEl.innerHTML = `<div class="error">약관을 불러오는 중 오류가 발생했습니다.<br>${error.message}</div>`;
        titleEl.textContent = "오류 발생";
    }
}