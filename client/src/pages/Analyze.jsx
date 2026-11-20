import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import FileDropzone from '../components/FileDropzone';
import FormField from '../components/FormField';
import { SAMPLE_JOB_DESCRIPTION } from '../data/sampleJobDescription';
import { createAnalysis } from '../services/analyses';
import { parseApiError } from '../services/errors';
import { uploadResume } from '../services/resumes';

const MAX_FILE_BYTES = 5 * 1024 * 1024;
const MAX_DESCRIPTION_LENGTH = 20000;

function validateFile(file) {
  if (!file) return 'Choose a PDF resume to upload.';
  if (file.type !== 'application/pdf') return 'Only PDF files are supported.';
  if (file.size === 0) return 'This file is empty.';
  if (file.size > MAX_FILE_BYTES) return 'This file is larger than 5 MB.';
  return '';
}

const BUTTON_LABELS = {
  idle: 'Analyze resume',
  uploading: 'Uploading resume...',
  analyzing: 'Analyzing...',
};

export default function Analyze() {
  const navigate = useNavigate();
  const [file, setFile] = useState(null);
  const [fileError, setFileError] = useState('');
  const [jobDescription, setJobDescription] = useState('');
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [status, setStatus] = useState('idle');
  // The resume that was already uploaded for the selected file, so a retry doesn't upload it twice.
  const [uploaded, setUploaded] = useState(null);

  const busy = status !== 'idle';

  const handleFileSelect = (selected) => {
    setFile(selected);
    setFileError(selected ? validateFile(selected) : '');
    setUploaded(null);
  };

  // Use the sample job description without silently replacing visitor text.
  const handleUseSample = () => {
    if (
      jobDescription.trim() &&
      !window.confirm(
        'Replace the job description you typed with the sample?'
      )
    ) {
      return;
    }

    setJobDescription(SAMPLE_JOB_DESCRIPTION);
    setFieldErrors({});
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    const problem = validateFile(file);
    if (problem) {
      setError('');
      setFileError(problem);
      return;
    }

    // "required" lets whitespace through, so check the trimmed text before uploading anything.
    const description = jobDescription.trim();
    if (!description) {
      setError('');
      setFieldErrors({
        jobDescriptionText: 'Paste the job description you are applying for.',
      });
      return;
    }

    setError('');
    setFieldErrors({});

    try {
      let resume = uploaded && uploaded.file === file ? uploaded : null;
      if (!resume) {
        setStatus('uploading');
        const result = await uploadResume(file);
        resume = { file, id: result.id };
        setUploaded(resume);
      }

      setStatus('analyzing');
      const analysis = await createAnalysis({
        resumeId: resume.id,
        jobDescriptionText: description,
      });
      navigate(`/analyses/${analysis.id}`);
    } catch (err) {
      const parsed = parseApiError(err);
      setError(parsed.message);
      setFieldErrors(parsed.fieldErrors);
      setStatus('idle');
    }
  };

  return (
    <section className="analyze">
      <header className="analyze-header">
        <h1>Analyze your resume</h1>
        <p className="intro">
          Upload your resume and paste the job description you are applying for. We compare the two
          and show you how well they match.
        </p>
      </header>

      {/* Sample files and job description shortcuts */}
      <div className="sample-help">
        <p>No resume handy? Try the sample files.</p>
        <div className="sample-actions">
          <a
            className="btn btn-secondary"
            href="/sample-resume.pdf"
            download="sample-resume.pdf"
          >
            Download sample resume
          </a>
          <button
            type="button"
            className="btn btn-secondary"
            disabled={busy}
            onClick={handleUseSample}
          >
            Use sample job description
          </button>
        </div>
      </div>

      {error && <p className="form-banner error" role="alert">{error}</p>}

      <form className="analyze-form" onSubmit={handleSubmit}>
        <FileDropzone
          name="resume"
          label="Resume"
          hint="PDF only, up to 5 MB. Scanned images without selectable text cannot be analyzed."
          accept="application/pdf"
          file={file}
          error={fileError}
          disabled={busy}
          onSelect={handleFileSelect}
        />
        <FormField
          as="textarea"
          label="Job description"
          name="jobDescriptionText"
          rows={10}
          required
          maxLength={MAX_DESCRIPTION_LENGTH}
          placeholder="Paste the full job description here"
          hint={`${jobDescription.length.toLocaleString()} / ${MAX_DESCRIPTION_LENGTH.toLocaleString()} characters`}
          value={jobDescription}
          onChange={(event) => setJobDescription(event.target.value)}
          error={fieldErrors.jobDescriptionText}
        />

        <div className="analyze-actions">
          <button type="submit" className="btn btn-primary btn-lg" disabled={busy}>
            {busy && <span className="spinner" aria-hidden="true" />}
            {BUTTON_LABELS[status]}
          </button>
          <div role="status">
            {busy && (
              <ol className="progress">
                <li className={status === 'uploading' ? 'progress-step active' : 'progress-step done'}>
                  Upload resume
                </li>
                <li className={status === 'analyzing' ? 'progress-step active' : 'progress-step'}>
                  Analyze match
                </li>
              </ol>
            )}
          </div>
        </div>
      </form>
    </section>
  );
}
