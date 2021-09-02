package elasticsearch.experiments.model;

import java.util.List;

public class CatalogItemUtil {
	
	public static List<CatalogItem> getCatalogItems() {
		Category electronics = new Category(null, "Electronics");
		Category houseElectronics = new Category(electronics, "House Electronics");
		Category officeElectronics = new Category(electronics, "Office Electronics");
		
		Manufacturer sony = new Manufacturer("Sony Corp", "1 Sony way, Some City");
		Manufacturer philips = new Manufacturer("Phillips Corp", "1 Phillips way, Some City");
		Manufacturer motorolla = new Manufacturer("Motorolla Corp", "1 Motorolla way, Some City");
		Manufacturer samsung = new Manufacturer("Samsung Corp", "1 Samsung way, Some City");
		
		CatalogItem flashLight = new CatalogItem(1, officeElectronics, "A green flashlight-1 uses 2 AA batteries", philips, 234.0, 1.99);
		CatalogItem flashLight2 = new CatalogItem(2, officeElectronics, "A blue flashlight-2 uses 2 AA batteries", philips, 234.0, 2.99);
		CatalogItem flashLight3 = new CatalogItem(3, officeElectronics, "A green flashlight-3 uses 2 AA batteries", philips, 234.0, 3.99);
		CatalogItem flashLight4 = new CatalogItem(4, officeElectronics, "A green flashlight-4 uses 2 AA batteries", philips, 234.0, 4.99);
		CatalogItem flashLight5 = new CatalogItem(5, houseElectronics, "A green flashlight-5 uses 2 AA batteries", philips, 234.0, 5.99);
		CatalogItem flashLight6 = new CatalogItem(6, officeElectronics, "A green flashlight-6 uses 2 AA batteries", philips, 234.0, 6.99);
		
		return List.of(flashLight, flashLight2, flashLight3, flashLight4, flashLight5, flashLight6);
	}

}
