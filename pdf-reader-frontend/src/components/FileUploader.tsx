import {useRef, ChangeEvent, useState} from "react";

interface FileUploaderProps {
    onUpload: (file: File) => void | Promise<void>;
    loading?: boolean;
    useAi: boolean;
    onAiChange: (value: boolean) => void;
}

const FileUploader = ({ onUpload, loading = false, useAi, onAiChange }: FileUploaderProps) => {
    const uploadRef = useRef<HTMLInputElement>(null)
    const [uploadedFile, setUploadedFile] = useState<string | null>(null)

    const handleUpload = (e: ChangeEvent<HTMLInputElement>) => {
        if(e.target.files === null){
            return
        }

        const file = e.target.files[0];
        if(file){
            if(file.type !== "application/pdf"){
                alert("Lütfen bir .pdf dosyası yükleyin")
                return
            }

            setUploadedFile(file.name)
            onUpload(file)
            e.target.value = ''
        }
        else{
            alert("Dosya yüklenemedi lütfen tekrar deneyin")
        }
    }

    return(
        <div className="file-uploader-container">
            <div>
                <button
                    className="file-upload-button"
                    onClick={() => uploadRef.current?.click()}
                    disabled={loading}
                >
                    {loading ? "Yükleniyor..." : "Upload PDF"}
                </button>
                <input
                    type="file"
                    ref={uploadRef}
                    accept=".pdf"
                    onChange={handleUpload}
                    style={{display:"none"}}
                />
                <label
                    className="ai-toggle"
                    style={{display:"flex", alignItems:"center", gap:"8px", marginTop:"12px", cursor:"pointer"}}
                >
                    <input
                        type="checkbox"
                        checked={useAi}
                        onChange={(e) => onAiChange(e.target.checked)}
                    />
                    <span className="text-white">AI ile analiz et (Gemini)</span>
                </label>
            </div>
            {loading && (
                <div className="loading-indicator" role="status" aria-live="polite">
                    <div className="spinner" aria-hidden="true" />
                    <p className="loading-text">Tarama yapılıyor...</p>
                </div>
            )}
            {uploadedFile && (
                <p className="text-white" style={{marginTop: "16px", fontSize: "0.95rem"}}>
                    ✓ Uploaded: <strong>{uploadedFile}</strong>
                </p>
            )}
        </div>
    )
}

export default FileUploader
