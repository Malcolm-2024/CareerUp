// 1. FRIENDLY CONNECTION ERRORS — never show browser/JDBC exception text in the page.
export const CONNECTION_MESSAGE = "CareerUp could not connect. Make sure the application is running, then try again.";
const TIMEOUT_MESSAGE = "CareerUp is taking too long to respond. Keep the application running and try again.";
function responseError(status, data) {
  const fallback = ({401:"Your session has ended. Please sign in again.",403:"This action is not available for your account. Refresh the page and try again.",413:"That upload is too large. Pictures must be under 10 MB; videos under 50 MB.",503:"CareerUp is temporarily unavailable. Keep the application running and try again."})[status]
    || (status >= 500 ? "CareerUp could not finish this action. Please try again." : "Unable to complete this action. Please try again.");
  const supplied = typeof (data?.message || data?.detail) === "string" ? (data.message || data.detail) : "";
  const message = ((supplied && status < 500) || (status === 503 && supplied.startsWith("CareerUp"))) ? supplied : fallback;
  const error = new Error(/ECONNREFUSED|connection refused|jdbc:|java\.|Failed to fetch|NetworkError/i.test(message) ? CONNECTION_MESSAGE : message);
  error.status = status;
  return error;
}

// 2. SHARED JSON REQUESTS — used by sign-in, all five roles and retrieval screens.
export async function request(path, options = {}) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 30000);
  try {
    const response = await fetch("/api" + path, {credentials:"same-origin", ...options, signal:options.signal || controller.signal});
    if (response.status === 204) return null;
    let data;
    try { data = await response.json(); }
    catch (error) {
      if (controller.signal.aborted) throw error;
      throw responseError(502,{message:"CareerUp returned an unexpected response. Refresh the page and try again."});
    }
    if (!response.ok) throw responseError(response.status,data);
    return data;
  } catch (error) {
    if (error.status) throw error;
    throw new Error(controller.signal.aborted ? TIMEOUT_MESSAGE : CONNECTION_MESSAGE);
  } finally { clearTimeout(timer); }
}

// 3. FILE RULES — match backend MediaController.java; server checks the actual file bytes too.
export function fileProblem(file) {
  if (!file) return "Choose a picture or video from your computer.";
  if (!file.size) return "That file is empty. Choose a different picture or video.";
  const extension = file.name.split(".").pop().toLowerCase();
  const image = ["jpg","jpeg","png","gif"].includes(extension);
  if (!image && !["mp4","webm"].includes(extension)) return "Choose a JPG, PNG or GIF picture, or an MP4 or WebM video. Convert HEIC/MOV files first.";
  if (file.size > (image ? 10 : 50) * 1024 * 1024) return image ? "That picture is too large. Choose one under 10 MB." : "That video is too large. Choose one under 50 MB.";
  return "";
}

// 4. UPLOAD PROGRESS — native browser API; no upload library or extra dependency.
export function upload(path, body, headers, onProgress) {
  return new Promise((resolve,reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open("POST","/api" + path);
    xhr.timeout = 120000;
    Object.entries(headers).forEach(([name,value]) => xhr.setRequestHeader(name,value));
    xhr.upload.onprogress = event => {if (event.lengthComputable) onProgress(Math.round(100 * event.loaded / event.total));};
    xhr.onerror = () => reject(new Error(CONNECTION_MESSAGE));
    xhr.ontimeout = () => reject(new Error("The upload took too long. Try a smaller file and publish again."));
    xhr.onabort = () => reject(new Error("The upload was interrupted. Choose your file and try again."));
    xhr.onload = () => {
      let data;
      try { data = JSON.parse(xhr.responseText); }
      catch { reject(new Error("CareerUp could not confirm the upload. Retry the same submission safely."));return; }
      if (xhr.status < 200 || xhr.status >= 300) reject(responseError(xhr.status,data));
      else resolve(data);
    };
    xhr.send(body);
  });
}
