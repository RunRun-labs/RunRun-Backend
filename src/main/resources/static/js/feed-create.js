document.addEventListener("DOMContentLoaded", () => {
    console.log("feed-create.js loaded");
    attachBackButtonHandler();
    attachImageUploadHandler();
    attachFormSubmitHandler();
    loadRunningRecordData();
});

/**
 * 뒤로가기 버튼 핸들러
 */
function attachBackButtonHandler() {
    const backButton = document.querySelector('[data-role="back-button"]');
    if (backButton) {
        backButton.addEventListener("click", () => {
            window.history.back();
        });
    }
}

/**
 * URL에서 runningResultId 가져오기
 */
function getRunningResultId() {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get('runningResultId');
}

/**
 * 러닝 기록 데이터 로드
 */
async function loadRunningRecordData() {
    const recordId = getRunningResultId();
    if (!recordId) {
        alert("러닝 기록 ID가 없습니다.");
        window.location.href = "/feed/records";
        return;
    }

    try {
        const token = localStorage.getItem("accessToken");
        if (!token) {
            window.location.href = "/login";
            return;
        }

        const response = await fetch(`/api/records/${recordId}/detail`, {
            headers: {Authorization: `Bearer ${token}`}
        });

        if (!response.ok) {
            if (response.status === 401) {
                window.location.href = "/login";
                return;
            }
            throw new Error("러닝 기록 조회 실패");
        }

        const payload = await response.json();
        const record = payload?.data;

        if (!record) {
            throw new Error("러닝 기록 데이터가 없습니다.");
        }

        // 데이터 표시
        renderRunningRecordData(record);

    } catch (error) {
        console.error("러닝 기록 로드 실패:", error);
        alert("러닝 기록을 불러오는데 실패했습니다.");
        window.location.href = "/feed/records";
    }
}

/**
 * 러닝 기록 데이터 렌더링
 */
function renderRunningRecordData(record) {
    // 코스 제목
    const courseTitleLabel = document.getElementById("courseTitleLabel");
    if (courseTitleLabel && record.courseTitle) {
        courseTitleLabel.textContent = `${record.courseTitle}`;
    }

    // 거리
    const distanceValue = document.getElementById("distanceValue");
    if (distanceValue && record.totalDistanceKm) {
        distanceValue.textContent = `${record.totalDistanceKm.toFixed(1)}km`;
    }

    // 시간
    const timeValue = document.getElementById("timeValue");
    if (timeValue && record.totalTimeSec) {
        timeValue.textContent = formatDuration(record.totalTimeSec);
    }

    // 평균 페이스
    const paceText = document.getElementById("paceText");
    if (paceText && record.avgPace) {
        const paceStr = formatPace(record.avgPace);
        paceText.textContent = `평균 페이스: ${paceStr}`;
    }

    // 이미지 (기본값: 코스 썸네일)
    const previewImage = document.getElementById("previewImage");
    const imagePlaceholder = document.getElementById("imagePlaceholder");
    if (record.courseThumbnailUrl) {
        previewImage.src = record.courseThumbnailUrl;
        previewImage.removeAttribute("hidden");
        if (imagePlaceholder) {
            imagePlaceholder.style.display = "none";
        }
    } else {
        if (imagePlaceholder) {
            imagePlaceholder.textContent = "이미지 없음";
        }
    }

    // runningResultId 저장 (폼 제출 시 사용)
    document.getElementById("feedPostForm").setAttribute("data-running-result-id", record.runningResultId);
}

/**
 * 시간 포맷팅 (초 -> MM:SS 또는 HH:MM:SS)
 */
function formatDuration(seconds) {
    if (!seconds || seconds === 0) return "00:00";
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;
    if (hours > 0) {
        return `${String(hours).padStart(2, "0")}:${String(minutes).padStart(2, "0")}:${String(secs).padStart(2, "0")}`;
    }
    return `${String(minutes).padStart(2, "0")}:${String(secs).padStart(2, "0")}`;
}

/**
 * 페이스 포맷팅 (분/km)
 */
function formatPace(pace) {
    if (!pace || pace === 0) return "-";
    const minutes = Math.floor(pace);
    const seconds = Math.floor((pace - minutes) * 60);
    return `${minutes}'${String(seconds).padStart(2, "0")}"`;
}

/**
 * 이미지 업로드 핸들러
 */
function attachImageUploadHandler() {
    const imageUploadButton = document.getElementById("imageUploadButton");
    const imageInput = document.getElementById("imageInput");
    const previewImage = document.getElementById("previewImage");
    const imagePlaceholder = document.getElementById("imagePlaceholder");

    if (!imageUploadButton || !imageInput || !previewImage) return;

    imageUploadButton.addEventListener("click", () => {
        imageInput.click();
    });

    imageInput.addEventListener("change", (e) => {
        const file = e.target.files[0];
        if (!file) return;

        // 파일 타입 검증
        if (!file.type.startsWith("image/")) {
            alert("이미지 파일만 업로드 가능합니다.");
            return;
        }

        // 파일 크기 검증 (10MB 제한)
        if (file.size > 10 * 1024 * 1024) {
            alert("이미지 크기는 10MB 이하여야 합니다.");
            return;
        }

        // 미리보기 표시
        const reader = new FileReader();
        reader.onload = (event) => {
            previewImage.src = event.target.result;
            previewImage.removeAttribute("hidden");
            if (imagePlaceholder) {
                imagePlaceholder.style.display = "none";
            }
        };
        reader.readAsDataURL(file);
    });
}

/**
 * 폼 제출 핸들러
 */
function attachFormSubmitHandler() {
    const form = document.getElementById("feedPostForm");
    const submitButton = document.getElementById("submitButton");

    if (!form || !submitButton) return;

    form.addEventListener("submit", async (e) => {
        e.preventDefault();

        const recordId = form.getAttribute("data-running-result-id");
        if (!recordId) {
            alert("러닝 기록 ID가 없습니다.");
            return;
        }

        const content = document.getElementById("contentInput").value.trim();
        const imageInput = document.getElementById("imageInput");
        const imageFile = imageInput.files[0];

        // 버튼 비활성화
        submitButton.disabled = true;
        submitButton.textContent = "등록 중...";

        try {
            const token = localStorage.getItem("accessToken");
            if (!token) {
                window.location.href = "/login";
                return;
            }

            // FormData 생성
            const formData = new FormData();
            formData.append("feedPost", new Blob([JSON.stringify({
                runningResultId: Number(recordId),
                content: content
            })], {type: "application/json"}));

            if (imageFile) {
                formData.append("imageFile", imageFile);
            }

            const response = await fetch("/api/feed", {
                method: "POST",
                headers: {
                    "Authorization": `Bearer ${token}`
                },
                body: formData
            });

            if (!response.ok) {
                if (response.status === 401) {
                    window.location.href = "/login";
                    return;
                }
                const error = await response.json();
                throw new Error(error?.message || "피드 공유 실패");
            }

            const result = await response.json();
            if (result.success) {
                alert("피드에 공유되었습니다!");
                window.location.href = "/feed";
            } else {
                throw new Error(result.message || "피드 공유 실패");
            }

        } catch (error) {
            console.error("피드 공유 실패:", error);
            alert(error.message || "피드 공유 중 오류가 발생했습니다.");
        } finally {
            submitButton.disabled = false;
            submitButton.textContent = "등록하기";
        }
    });
}
