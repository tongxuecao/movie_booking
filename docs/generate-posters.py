"""
为每部电影生成带标题的彩色占位海报图
运行方式: python docs/generate-posters.py
需要: pip install Pillow
生成后保存到 backend/uploads/ 目录
"""
import os
from PIL import Image, ImageDraw, ImageFont

UPLOAD_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'backend', 'uploads')
os.makedirs(UPLOAD_DIR, exist_ok=True)

# (文件名, 标题, 副标题, 背景色)
MOVIES = [
    ('poster_ld3.jpg',   '流浪地球3',       '科幻 / 冒险',       (15, 32, 65)),
    ('poster_tj4.jpg',   '唐人街探案4',     '喜剧 / 悬疑',       (200, 50, 30)),
    ('poster_fs3.jpg',   '封神第三部',       '奇幻 / 动作',       (120, 30, 50)),
    ('poster_rlgd2.jpg', '热辣滚烫2',       '喜剧 / 剧情',       (230, 100, 40)),
    ('poster_nz2.jpg',   '哪吒之魔童闹海', '动画 / 奇幻',       (180, 50, 80)),
    ('poster_st.jpg',    '三体',            '科幻 / 悬疑',       (20, 20, 40)),
    ('poster_mjh.jpg',   '满江红',          '悬疑 / 历史',       (160, 40, 30)),
    ('poster_gzyz.jpg',  '孤注一掷',        '犯罪 / 剧情',       (40, 40, 50)),
    ('poster_ca.jpg',    '长安三万里',      '动画 / 历史',       (60, 80, 120)),
    ('poster_xstd.jpg',  '消失的她',        '悬疑 / 犯罪',       (80, 50, 100)),
]

W, H = 400, 560

# 尝试加载系统中文字体
FONT_PATHS = [
    'C:/Windows/Fonts/msyh.ttc',    # 微软雅黑
    'C:/Windows/Fonts/simhei.ttf',  # 黑体
    'C:/Windows/Fonts/simsun.ttc',  # 宋体
]

font_big = None
font_small = None
for fp in FONT_PATHS:
    if os.path.exists(fp):
        try:
            font_big = ImageFont.truetype(fp, 42)
            font_small = ImageFont.truetype(fp, 22)
            break
        except:
            pass

if font_big is None:
    font_big = ImageFont.load_default()
    font_small = ImageFont.load_default()

for filename, title, subtitle, bg in MOVIES:
    filepath = os.path.join(UPLOAD_DIR, filename)
    if os.path.exists(filepath):
        print(f'[跳过] {filename} 已存在')
        continue

    img = Image.new('RGB', (W, H), bg)
    draw = ImageDraw.Draw(img)

    # 渐变效果 (简单版)
    for y in range(H):
        alpha = y / H
        r = int(bg[0] * (1 - alpha * 0.3))
        g = int(bg[1] * (1 - alpha * 0.3))
        b = int(bg[2] * (1 - alpha * 0.3))
        draw.line([(0, y), (W, y)], fill=(r, g, b))

    # 装饰线
    draw.rectangle([30, 180, W - 30, 183], fill=(255, 255, 255, 128))
    draw.rectangle([30, 380, W - 30, 383], fill=(255, 255, 255, 80))

    # 标题
    bbox = draw.textbbox((0, 0), title, font=font_big)
    tw = bbox[2] - bbox[0]
    draw.text(((W - tw) // 2, 220), title, fill='white', font=font_big)

    # 副标题
    bbox2 = draw.textbbox((0, 0), subtitle, font=font_small)
    tw2 = bbox2[2] - bbox2[0]
    draw.text(((W - tw2) // 2, 310), subtitle, fill=(200, 200, 200), font=font_small)

    # 底部标识
    draw.text((30, H - 60), 'MovieTicket', fill=(255, 255, 255, 128), font=font_small)

    img.save(filepath, 'JPEG', quality=90)
    print(f'[成功] {filename}')

print('海报生成完成!')
