针对你们的电影订票系统大作业项目，你和前端同学的最佳协同方案是：**两人共享同一个远程仓库，但各自在独立的分支上开发，最后由你（作为后端兼架构师）来进行代码合并。**

为了防止你们的代码互相覆盖或产生冲突，以下是为你们量身定制的**保姆级可执行步骤**：

## 🛠️ 第一阶段：作为组长（你）需要做的准备

在让前端同学拉取代码之前，你需要先在远程（GitHub/Gitee）做好配置，并把现有的后端架构推上去。

### 1. 邀请前端同学加入仓库（网页端操作）

- **GitHub 步骤**：进入你的 `movie_booking` 仓库 $\rightarrow$ 点击 **Settings** $\rightarrow$ **Collaborators** $\rightarrow$ 点击 **Add people** $\rightarrow$ 输入前端同学的 GitHub 账号或邮箱邀请他。
- **Gitee 步骤**：进入仓库 $\rightarrow$ **管理** $\rightarrow$ **仓库成员管理** $\rightarrow$ **开发者** $\rightarrow$ **添加仓库成员**。

> *注意：他必须接受邀请后，才拥有向这个仓库上传代码的权限。*

### 2. 确保你的后端代码已成功推送到主分支（本地终端）

确保你本地的 `main` 分支是干净且最新的，并且已经推送到远程：

Bash

```
git status   # 确保没有未提交的修改
git push origin main
```

## 🚀 第二阶段：前端同学的执行步骤（前端看这里）

现在，前端同学可以上场了。请把以下步骤直接转发给他：

### 1. 克隆项目到本地

前端同学找一个干净的电脑目录，打开 Git Bash 执行：

Bash

```
git clone <你们仓库的URL地址>
cd movie_booking
```

### 2. 【核心】创建属于前端自己的独立分支

**切记：不要直接在 `main` 分支上写前端代码！** 前端同学需要创建一个名为 `frontend` 的新分支，并切换过去：

Bash

```
git checkout -b frontend
# 或者用现代命令：git switch -c frontend
```

### 3. 编写代码并上传前端内容

前端同学可以在项目根目录下创建一个 `frontend`（或 `movie_booking_ui`）文件夹，把他的前端代码放进去。

当他写完一部分功能，想要上传时，在本地终端执行：

Bash

```
# 1. 查看状态，确保只有前端文件被修改或添加
git status

# 2. 将前端代码添加到暂存区
git add .

# 3. 提交到本地仓库
git commit -m "feat: 完成前端登录与电影列表页面"

# 4. 【关键】将代码推送到远程的 frontend 分支（不要推给 main）
git push -u origin frontend
```

执行完后，代码就会安全地躺在远程的 `frontend` 分支上，绝对不会影响你原本的后端 `main` 分支。

## 🔄 第三阶段：你（后端组长）如何拉取他的前端代码并合并

当前端同学通知你“我把前端代码传上去了”，你作为项目负责人，需要把他的代码拉下来合并到主线中。

### 1. 更新远程分支信息并查看

在你的本地终端执行，让你的本地 Git 知道远程多了一个 `frontend` 分支：

Bash

```
git fetch origin
git branch -a  # 你应该能看到 origin/frontend 了
```

### 2. 合并前端代码到主分支（Main）

确保你当前在 `main` 分支上，然后把远程的前端分支合并进来：

Bash

```
# 1. 切换到主分支
git checkout main

# 2. 稳妥起见，先拉取一次主分支最新代码
git pull origin main

# 3. 把远程前端同学的分支合并到你当前的主分支
git merge origin/frontend
```

*注：由于你们一个是后端（Java/Spring Boot），一个是前端（Vue/React/HTML），文件路径完全错开，Git 会 **100% 自动完美合并**，绝对不会产生代码冲突（Conflict）。*

### 3. 把合并后的完整项目推送到主线

合并成功后，你本地的 `main` 分支就同时拥有了前端和后端的完整代码。最后把它推送到远程：

Bash

```
git push origin main
```

以后前端同学有了新更新，他继续在他的 `frontend` 分支 `git push`，你重复**第三阶段**的步骤进行 `merge` 即可。整个大作业的协同开发流程就能跑得极其顺畅！