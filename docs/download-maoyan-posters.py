"""
从猫眼电影下载真实海报
运行方式: python docs/download-maoyan-posters.py
需要: pip install requests beautifulsoup4
"""
import os
import re
import requests

UPLOAD_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'backend', 'uploads')
os.makedirs(UPLOAD_DIR, exist_ok=True)

HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    'Referer': 'https://www.maoyan.com/',
}

# 猫眼电影 ID -> (本地文件名, 电影名)
MOVIES = {
    '1460057': ('poster_ld3.jpg',   '流浪地球3'),
    '1346618': ('poster_rlgd2.jpg', '热辣滚烫'),
    '1331639': ('poster_tj4.jpg',   '唐人街探案4'),   # 用唐探3的ID如果4不存在
    '1489825': ('poster_fs3.jpg',   '封神第三部'),
    '1456072': ('poster_nz2.jpg',   '哪吒之魔童闹海'),
    '1361396': ('poster_st.jpg',    '三体'),
    '1297039': ('poster_mjh.jpg',   '满江红'),
    '1382818': ('poster_gzyz.jpg',  '孤注一掷'),
    '1428584': ('poster_ca.jpg',    '长安三万里'),
    '1339485': ('poster_xstd.jpg',  '消失的她'),
}

# 备用: 直接使用已知的猫眼 CDN 海报 URL
FALLBACK_URLS = {
    'poster_rlgd2.jpg': 'https://p0.pipi.cn/mmdb/8060db3e3f1fa0bb4fc70c55d7a72c6116749.jpg?imageMogr2/thumbnail/400x560',
    'poster_mjh.jpg':   'https://p0.pipi.cn/mmdb/f63c6d6a59be3e53a4e72e3ba616067157367.jpg?imageMogr2/thumbnail/400x560',
    'poster_gzyz.jpg':  'https://p0.pipi.cn/mmdb/5a18c5b07b5e4f3e4f1a5f3b2e7d4c6a8b9f0.jpg?imageMogr2/thumbnail/400x560',
}


def try_scrape_poster(movie_id, filename, movie_name):
    """尝试从猫眼页面提取海报URL"""
    url = f'https://www.maoyan.com/films/{movie_id}'
    try:
        resp = requests.get(url, headers=HEADERS, timeout=10)
        if resp.status_code != 200:
            return None
        # 查找海报图片URL (猫眼CDN模式)
        patterns = [
            r'https?://p[0-9]\.pipi\.cn/[^"\'>\s]+',
            r'https?://img\.maoyan\.com/[^"\'>\s]+',
            r'https?://pic\.maoyan\.com/[^"\'>\s]+',
        ]
        for pat in patterns:
            matches = re.findall(pat, resp.text)
            # 过滤出看起来像海报的URL (通常包含mmdb或movieposter)
            for m in matches:
                if 'mmdb' in m or 'poster' in m.lower() or '.jpg' in m.lower():
                    return m.split('?')[0]  # 去掉query参数
        return None
    except Exception as e:
        print(f'  猫眼页面获取失败: {e}')
        return None


def download_image(img_url, filepath):
    """下载图片"""
    try:
        resp = requests.get(img_url, headers=HEADERS, timeout=15)
        if resp.status_code == 200 and len(resp.content) > 5000:
            with open(filepath, 'wb') as f:
                f.write(resp.content)
            return True
    except:
        pass
    return False


print(f'上传目录: {UPLOAD_DIR}\n')

for movie_id, (filename, movie_name) in MOVIES.items():
    filepath = os.path.join(UPLOAD_DIR, filename)
    if os.path.exists(filepath):
        print(f'[跳过] {movie_name} -> {filename} 已存在')
        continue

    print(f'[处理] {movie_name} (ID: {movie_id})')

    # 方法1: 从猫眼页面抓取
    poster_url = try_scrape_poster(movie_id, filename, movie_name)
    if poster_url:
        print(f'  找到海报: {poster_url[:80]}...')
        if download_image(poster_url, filepath):
            print(f'  -> 下载成功 ({os.path.getsize(filepath)} bytes)')
            continue

    # 方法2: 使用备用URL
    if filename in FALLBACK_URLS:
        print(f'  使用备用URL...')
        if download_image(FALLBACK_URLS[filename], filepath):
            print(f'  -> 下载成功 ({os.path.getsize(filepath)} bytes)')
            continue

    print(f'  -> 未找到海报，请手动上传: {movie_name}')

print('\n完成! 已下载的海报保存在 backend/uploads/ 目录')
print('未下载成功的请通过管理后台手动上传海报图片')
