import yt_dlp

final_filename = None  # Global variable

def download_video_audio(url, folder_path, completion_callback):
    global final_filename

    output_template = f'{folder_path}/%(title)s.%(ext)s'

    def progress_hook(d):
        global final_filename
        if d['status'] == 'finished':
            # Store the final filename in the global variable
            final_filename = d.get('info_dict').get('_filename')
            # Call the Java Runnable, but don't pass any arguments
            completion_callback()

    ydl_opts = {
        'format': 'best',            # Download the best available video+audio
        'outtmpl': output_template,  # Save the file to the specified location
        'progress_hooks': [progress_hook]  # Hook to track download progress
    }

    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        ydl.download(url)
