package hello.itemservice.domain.item;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ItemRepositoryTest {

    ItemRepository itemRepository = new ItemRepository();

    @AfterEach
    void afterEach(){
        itemRepository.clearStore();
    }

    @Test
    public void save() throws Exception {
        // given
        Item item = new Item("itemA", 10000, 10);
        // when
        Item savedITem = itemRepository.save(item);
        // then
        Item findItem = itemRepository.findById(savedITem.getId());
        assertThat(findItem).isEqualTo(savedITem);
    }

    @Test
    public void findAll() throws Exception {
        // given
        Item item1 = new Item("itemA", 10000, 10);
        Item item2 = new Item("itemB", 20000, 20);
        itemRepository.save(item1);
        itemRepository.save(item2);
        // when
        List<Item> all = itemRepository.findAll();
        // then
        assertThat(all.size()).isEqualTo(2);
        assertThat(all).contains(item1, item2);
    }

    @Test
    public void updateItem() throws Exception {
        // given
        Item item = new Item("itemA", 10000, 10);
        Item savedItem = itemRepository.save(item);
        Long itemId = savedItem.getId();

        // when
        Item updateParam = new Item("itemB", 20000, 30);
        itemRepository.update(itemId, updateParam);

        // then
        Item findItem = itemRepository.findById(itemId);
        assertThat(findItem.getItemName()).isEqualTo(updateParam.getItemName());
        assertThat(findItem.getQuantity()).isEqualTo(updateParam.getPrice());
        assertThat(findItem.getQuantity()).isEqualTo(updateParam.getQuantity());
    }
}