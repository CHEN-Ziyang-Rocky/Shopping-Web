-- Optional demo data, run only after Flyway. Never resets existing price/stock.
-- Run one seeder at a time while shop-api is stopped; not an application migration.
START TRANSACTION;
INSERT INTO categories(name) SELECT 'Portfolio Demo' WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name='Portfolio Demo');
SET @demo_category = (SELECT MIN(catid) FROM categories WHERE name='Portfolio Demo');
INSERT INTO products(catid,name,price,description,stock,thumbnail_image,large_image)
SELECT @demo_category,'Demo Desk Lamp',129.00,'Portfolio demonstration item; test payments only.',10,'/uploads/demo-lamp.svg','/uploads/demo-lamp.svg'
WHERE NOT EXISTS(SELECT 1 FROM products WHERE catid=@demo_category AND name='Demo Desk Lamp');
INSERT INTO products(catid,name,price,description,stock,thumbnail_image,large_image)
SELECT @demo_category,'Demo Notebook',29.90,'Portfolio demonstration item; test payments only.',25,'/uploads/demo-notebook.svg','/uploads/demo-notebook.svg'
WHERE NOT EXISTS(SELECT 1 FROM products WHERE catid=@demo_category AND name='Demo Notebook');
COMMIT;
