INSERT INTO image_map_objects VALUES({id},{image_map_id},{description},{navigation_index},{outline_coordinates}::double precision[],{overlay_image_id})
ON CONFLICT ON CONSTRAINT image_map_objects_pk
  DO UPDATE SET description={description},navigation_index={navigation_index},outline_coordinates={outline_coordinates}::double precision[],overlay_image_id={overlay_image_id}