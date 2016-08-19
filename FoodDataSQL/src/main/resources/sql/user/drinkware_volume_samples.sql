SELECT drinkware_scale_id as scale_id, fill, volume 
FROM drinkware_volume_samples 
WHERE drinkware_scale_id IN ({scale_ids}) ORDER BY scale_id, fill