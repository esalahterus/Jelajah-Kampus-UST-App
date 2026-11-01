<?php
require_once __DIR__ . '/../helpers.php';
require_method('GET');

auth_user(false);

$res = $db->query("SELECT b.id, b.name, b.description, b.address, l.latitude, l.longitude
                   FROM buildings b LEFT JOIN locations l ON l.building_id = b.id ORDER BY b.id");
$buildings = [];
while ($b = $res->fetch_assoc()) {
    $bid = (int)$b['id'];

    $facilities = [];
    $r = $db->query("SELECT name, description FROM facilities WHERE building_id = $bid ORDER BY id");
    while ($f = $r->fetch_assoc()) $facilities[] = $f;

    $media = [];
    $r = $db->query("SELECT id, title, media_type, media_url FROM media_guides WHERE building_id = $bid ORDER BY id");
    while ($m = $r->fetch_assoc()) {
        $media[] = [
            'id' => (int)$m['id'],
            'title' => $m['title'],
            'media_type' => $m['media_type'],
            'media_url' => absolute_url($m['media_url']),
        ];
    }

    $buildings[] = [
        'id' => $bid,
        'name' => $b['name'],
        'description' => $b['description'],
        'address' => $b['address'],
        'latitude' => $b['latitude'] !== null ? (float)$b['latitude'] : null,
        'longitude' => $b['longitude'] !== null ? (float)$b['longitude'] : null,
        'facilities' => $facilities,
        'media' => $media,
    ];
}
json_out(['success' => true, 'buildings' => $buildings]);
