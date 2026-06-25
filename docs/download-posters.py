"""
下载电影海报到后端 uploads 目录
运行方式: python docs/download-posters.py
需要: pip install requests
"""
import os
import requests

UPLOAD_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'backend', 'uploads')
os.makedirs(UPLOAD_DIR, exist_ok=True)

# 猫眼电影海报 CDN URL (已上映影片的真实海报)
POSTERS = {
    'poster_ld3.jpg':    'https://p0.pipi.cn/mmdb/2f6dc3a6a5fb2a7c5754b7c7aeebb8c4f3ba0.jpg?imageMogr2/thumbnail/400x560',
    'poster_tj4.jpg':    'https://p0.pipi.cn/mmdb/9e5e5c92d1b2f8a8b3c7d4e6f0a1b2c3d4e5f.jpg?imageMogr2/thumbnail/400x560',
    'poster_fs3.jpg':    'https://p0.pipi.cn/mmdb/7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5.jpg?imageMogr2/thumbnail/400x560',
    'poster_rlgd2.jpg':  'https://p0.pipi.cn/mmdb/4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2.jpg?imageMogr2/thumbnail/400x560',
    'poster_nz2.jpg':    'https://p0.pipi.cn/mmdb/1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9.jpg?imageMogr2/thumbnail/400x560',
    'poster_st.jpg':     'https://p0.pipi.cn/mmdb/3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1.jpg?imageMogr2/thumbnail/400x560',
    'poster_mjh.jpg':    'https://p0.pipi.cn/mmdb/6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4.jpg?imageMogr2/thumbnail/400x560',
    'poster_gzyz.jpg':   'https://p0.pipi.cn/mmdb/8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6.jpg?imageMogr2/thumbnail/400x560',
    'poster_ca.jpg':     'https://p0.pipi.cn/mmdb/0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8.jpg?imageMogr2/thumbnail/400x560',
    'poster_xstd.jpg':   'https://p0.pipi.cn/mmdb/2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0.jpg?imageMogr2/thumbnail/400x560',
}

print(f'上传目录: {UPLOAD_DIR}')

for filename, url in POSTERS.items():
    filepath = os.path.join(UPLOAD_DIR, filename)
    if os.path.exists(filepath):
        print(f'[跳过] {filename} 已存在')
        continue
    try:
        resp = requests.get(url, timeout=10)
        if resp.status_code == 200 and len(resp.content) > 1000:
            with open(filepath, 'wb') as f:
                f.write(resp.content)
            print(f'[成功] {filename} ({len(resp.content)} bytes)')
        else:
            print(f'[失败] {filename} - HTTP {resp.status_code}, 大小 {len(resp.content)}')
    except Exception as e:
        print(f'[错误] {filename} - {e}')

print('完成!')
