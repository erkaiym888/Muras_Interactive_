# GitHub для двух новичков

## Один раз
1. Владелец репозитория GitHub: Settings → Collaborators → Add people → пригласить второго разработчика.
2. Второй принимает приглашение.
3. Обе устанавливают Git и VS Code или IntelliJ IDEA.
4. Каждая выполняет:

```bash
git clone https://github.com/erkaiym888/Muras_Interactive_.git
cd Muras_Interactive_
```

Владелец сначала добавляет архитектуру в `main`, затем обе выполняют `git pull origin main`.

## Ветка Эркайым
```bash
git switch main
git pull origin main
git switch -c feature/yurt
git add .
git commit -m "Add yurt module"
git push -u origin feature/yurt
```

## Ветка Адинай
```bash
git switch main
git pull origin main
git switch -c feature/auth-rites
git add .
git commit -m "Add auth and rites modules"
git push -u origin feature/auth-rites
```

`git switch -c` используют только при первом создании ветки. Потом: `git switch feature/yurt` или `git switch feature/auth-rites`.

## Каждый следующий раз
```bash
git status
git add .
git commit -m "Describe changes"
git push
```

После push: GitHub → Pull requests → New pull request → выбрать свою ветку → Create pull request. Вторая участница проверяет, после чего объединяете в `main`. Перед новым заданием обновите `main` и согласуйте перенос изменений в рабочую ветку.

Важно: если репозиторий приватный, без приглашения появится `Repository not found`. Убедитесь, что адрес точный и вы вошли в нужный GitHub-аккаунт.
