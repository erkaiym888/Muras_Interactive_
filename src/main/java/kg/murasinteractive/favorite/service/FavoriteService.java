package kg.murasinteractive.favorite.service;

import kg.murasinteractive.favorite.dto.FavoriteDto;
import kg.murasinteractive.favorite.model.Favorite;
import kg.murasinteractive.favorite.model.FavoriteType;
import kg.murasinteractive.favorite.repository.FavoriteRepository;
import kg.murasinteractive.folklore.repository.FolkloreHeroRepository;
import kg.murasinteractive.folklore.repository.FolkloreWorkRepository;
import kg.murasinteractive.rite.model.Rite;
import kg.murasinteractive.rite.repository.RiteRepository;
import kg.murasinteractive.user.model.UserAccount;
import kg.murasinteractive.user.repository.UserAccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserAccountRepository userRepository;
    private final RiteRepository riteRepository;
    private final FolkloreHeroRepository heroRepository;
    private final FolkloreWorkRepository workRepository;

    public FavoriteService(FavoriteRepository favoriteRepository, UserAccountRepository userRepository,
                           RiteRepository riteRepository, FolkloreHeroRepository heroRepository,
                           FolkloreWorkRepository workRepository) {
        this.favoriteRepository = favoriteRepository;
        this.userRepository = userRepository;
        this.riteRepository = riteRepository;
        this.heroRepository = heroRepository;
        this.workRepository = workRepository;
    }

    /** Список избранного, новые сверху. Материалы, которых уже нет, пропускаются. */
    @Transactional(readOnly = true)
    public List<FavoriteDto> list(UserAccount user) {
        return favoriteRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(f -> describe(f.getItemType(), f.getItemId()))
                .flatMap(Optional::stream)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isFavorite(UserAccount user, FavoriteType type, Long itemId) {
        return favoriteRepository.existsByUserAndItemTypeAndItemId(user, type, itemId);
    }

    /** Добавляет материал в избранное. Повторное добавление ничего не меняет. */
    @Transactional
    public void add(UserAccount user, FavoriteType type, Long itemId) {
        describe(type, itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Материал не найден"));
        if (favoriteRepository.existsByUserAndItemTypeAndItemId(user, type, itemId)) {
            return;
        }
        UserAccount owner = userRepository.getReferenceById(user.getUserId());
        try {
            favoriteRepository.saveAndFlush(new Favorite(owner, type, itemId));
        } catch (DataIntegrityViolationException e) {
            // два одинаковых запроса одновременно: запись уже есть
        }
    }

    @Transactional
    public void remove(UserAccount user, FavoriteType type, Long itemId) {
        favoriteRepository.deleteByUserAndItemTypeAndItemId(user, type, itemId);
    }

    private Optional<FavoriteDto> describe(FavoriteType type, Long id) {
        return switch (type) {
            case RITE -> riteRepository.findByRiteIdAndStatus(id, Rite.STATUS_PUBLISHED)
                    .map(r -> new FavoriteDto(type.name(), id, r.getTitle(), r.getRiteCategory().getCategoryName()));
            case HERO -> heroRepository.findById(id)
                    .map(h -> new FavoriteDto(type.name(), id, h.getName(), h.getHeroType().getTypeName()));
            case WORK -> workRepository.findById(id)
                    .map(w -> new FavoriteDto(type.name(), id, w.getTitle(), w.getWorkType().getTypeName()));
        };
    }
}
