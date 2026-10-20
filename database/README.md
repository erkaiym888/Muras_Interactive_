# База данных — этап 1

Таблицы для первого этапа по ТЗ:
- `user_role` (role_id, role_name)
- `user_account` (user_id, email, password_hash, display_name, role_id, total_points, registered_at)
- `rite_category` (rite_category_id, category_name)
- `rite` (rite_id, rite_category_id, title, description, status)
- `rite_step` (rite_step_id, rite_id, step_order, step_text)
- `yurt_element` (yurt_element_id, name, description, map_coordinates)
- `media_file` (media_id, file_path, media_type, caption)

Это перечень, не готовая SQL-миграция. Связи медиа и материалов, источники, символика и назначение элемента требуют дополнительного моделирования. Таблица `content_translation` предусмотрена ТЗ для дальнейшего расширения языков.
