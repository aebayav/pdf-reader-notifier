import { useRef, ChangeEvent, useState, DragEvent } from "react";

interface FileUploaderProps {
    onUpload: (file: File) => void | Promise<void>;
    groups: { id: string; name: string }[];
    selectedGroupId: string;
    onGroupChange: (value: string) => void;
    loading?: boolean;
    useAi: boolean;
    onAiChange: (value: boolean) => void;
}

const FileUploader = ({ onUpload, groups, selectedGroupId, onGroupChange, loading = false, useAi, onAiChange }: FileUploaderProps) => {
    const uploadRef = useRef<HTMLInputElement>(null)
    const [uploadedFile, setUploadedFile] = useState<string | null>(null)
    const [dragOver, setDragOver] = useState(false)
    const [fileError, setFileError] = useState<string | null>(null)

    const processFile = (file: File) => {
        setFileError(null)
        if (file.type !== "application/pdf" && !file.name.toLowerCase().endsWith(".pdf")) {
            setFileError("Lütfen bir .pdf dosyası yükleyin.")
            return
        }
        if (file.size > 50 * 1024 * 1024) {
            setFileError("Dosya boyutu 50 MB sınırını aşıyor.")
            return
        }
        setUploadedFile(file.name)
        onUpload(file)
    }

    const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0]
        if (file) processFile(file)
        e.target.value = ""
    }

    const handleDrop = (e: DragEvent<HTMLDivElement>) => {
        e.preventDefault()
        setDragOver(false)
        const file = e.dataTransfer.files?.[0]
        if (file) processFile(file)
    }

    const handleDragOver = (e: DragEvent<HTMLDivElement>) => {
        e.preventDefault()
        setDragOver(true)
    }

    const handleDragLeave = () => setDragOver(false)

    return (
        <div className="file-uploader-container">
            <div
                className={`upload-drop-zone${dragOver ? " drag-over" : ""}${loading ? " uploading" : ""}`}
                onDrop={handleDrop}
                onDragOver={handleDragOver}
                onDragLeave={handleDragLeave}
                onClick={() => !loading && uploadRef.current?.click()}
                role="button"
                tabIndex={0}
                aria-label="PDF yükle"
                onKeyDown={(e) => e.key === "Enter" && !loading && uploadRef.current?.click()}
            >
                <input
                    type="file"
                    ref={uploadRef}
                    accept=".pdf"
                    onChange={handleChange}
                    style={{ display: "none" }}
                />

                {loading ? (
                    <div className="loading-indicator" role="status" aria-live="polite">
                        <div className="spinner" aria-hidden="true" />
                        <p className="loading-text">Taranıyor...</p>
                    </div>
                ) : (
                    <>
                        <span className="upload-icon" aria-hidden="true">📤</span>
                        <p className="upload-primary-text">
                            {dragOver ? "Dosyayı bırakın" : "PDF Yükle"}
                        </p>
                        <p className="upload-secondary-text">
                            Tıklayın veya dosyayı buraya sürükleyin · Maks. 50 MB
                        </p>
                    </>
                )}
            </div>

            {uploadedFile && !loading && (
                <p className="upload-success-text">
                    ✓ Yüklendi: <strong>{uploadedFile}</strong>
                </p>
            )}


            <label className="ai-toggle-label">
                <input
                    type="checkbox"
                    className="ai-toggle-checkbox"
                    checked={useAi}
                    onChange={(e) => onAiChange(e.target.checked)}
                />
                <span className="ai-toggle-track" aria-hidden="true" />
                <span className="ai-toggle-text">✨ Gemini AI ile analiz et</span>
            </label>

            <label className="group-select-label">
                <span>Dosyayı gruba ekle</span>
                <select value={selectedGroupId} onChange={(e) => onGroupChange(e.target.value)} disabled={loading}>
                    <option value="">Gruplanmamış</option>
                    {groups.map((group) => <option key={group.id} value={group.id}>{group.name}</option>)}
                </select>
            </label>
        </div>
    )
}

export default FileUploader
