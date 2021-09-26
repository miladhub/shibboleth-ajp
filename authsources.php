<?php

$config = array(

    'admin' => array(
        'core:AdminPassword',
    ),

    'example-userpass' => array(
        'exampleauth:UserPass',
        'user1:user1pass' => array(
            'uid' => array('1'),
            'eduPersonAffiliation' => array('group1'),
            'email' => 'user1@example.com',
        ),
        'user2:user2pass' => array(
            'uid' => array('2'),
            'eduPersonAffiliation' => array('group2'),
            'email' => 'user2@example.com',
        ),
	    'admin:admin' => array(
            'uid' => array('admin'),
            'eduPersonAffiliation' => array('administrator', 'ShibDemo', "app-developer"),
            'email' => 'admin@example.com',
        ),
        'aUser:aUser' => array(
            'uid' => array('aUser'),
            'eduPersonAffiliation' => array('ShibDemo'),
            'email' => 'aUser@example.com',
        ),
    ),

);
