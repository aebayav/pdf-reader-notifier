import {useRef, ChangeEvent, useState} from "react";

const FileUploader = () => {
    const uploadRef = useRef<HTMLInputElement>(null)
    const [uploadedFile, setUploadedFile] = useState<string | null>(null)
    
    const handleUpload = (e: ChangeEvent<HTMLInputElement>) =>{
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
            
            const fileReader = new FileReader()
            fileReader.onload = (event) => {
                const contents = event?.target?.result
                console.log(URL.createObjectURL(file))
                console.log(contents)
                //Burada backende OCR için dosya yollanacak şimdilik boş.
            }
            e.target.value = ''
            fileReader.readAsArrayBuffer(file)
        }
        else{
            alert("Dosya yüklenemedi lütfen tekrar deneyin")
        }
    }
    
    return(
        <div className="file-uploader-container">
            <div>
                <button className="file-upload-button" onClick={() => uploadRef.current?.click()}>
                    Upload PDF
                </button>
                <input
                    type="file"
                    ref={uploadRef}
                    accept=".pdf"
                    onChange={handleUpload}
                    style={{display:"none"}}
                />
            </div>
            {uploadedFile && (
                <p className="text-white" style={{marginTop: "16px", fontSize: "0.95rem"}}>
                    ✓ Uploaded: <strong>{uploadedFile}</strong>
                </p>
            )}
        </div>
    )
}

export default FileUploader
