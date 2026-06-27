create table if not exists cinemas
(
    id             bigint auto_increment
        primary key,
    name           varchar(200)                          not null comment '影院名称',
    address        varchar(500)                          null comment '地址',
    phone          varchar(20)                           null comment '联系电话',
    status         varchar(15) default 'open'            null comment '状态: open/suspended/preparing/closed',
    business_hours varchar(100)                          null comment '营业时间',
    longitude      decimal(10, 7)                        null comment '经度',
    latitude       decimal(10, 7)                        null comment '纬度',
    created_at     datetime    default CURRENT_TIMESTAMP null,
    updated_at     datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP
)
    comment '影院表';

create table if not exists halls
(
    id         bigint auto_increment
        primary key,
    cinema_id  bigint                                not null comment '影院ID',
    name       varchar(50)                           not null comment '影厅名称',
    seat_rows  int                                   null comment '座位行数',
    seat_cols  int                                   null comment '座位列数',
    hall_type  varchar(10) default 'normal'          null comment '影厅类型: normal/imax/vip/threeD',
    created_at datetime    default CURRENT_TIMESTAMP null,
    updated_at datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    constraint halls_ibfk_1
        foreign key (cinema_id) references cinemas (id)
            on delete cascade
)
    comment '影厅表';

create index idx_cinema_id
    on halls (cinema_id);

create table if not exists movies
(
    id           bigint auto_increment
        primary key,
    title        varchar(200)                          not null comment '电影名称',
    description  text                                  null comment '电影简介',
    duration     int                                   null comment '时长(分钟)',
    release_date date                                  null comment '上映日期',
    poster       varchar(255)                          null comment '海报URL',
    rating       decimal(3, 1)                         null comment '评分(0.0-9.9)',
    genre        varchar(200)                          null comment '类型(动作/喜剧/科幻等)',
    director     varchar(100)                          null comment '导演',
    actors       varchar(500)                          null comment '主演(逗号分隔)',
    status       varchar(15) default 'upcoming'        null comment '状态: upcoming/showing/ended',
    created_at   datetime    default CURRENT_TIMESTAMP null,
    updated_at   datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP
)
    comment '电影表';

create table if not exists movie_images
(
    id         bigint auto_increment
        primary key,
    movie_id   bigint                                not null comment '电影ID',
    image_url  varchar(255)                          not null comment '图片URL',
    image_type varchar(10) default 'poster'          null comment '图片类型: poster/still/banner',
    sort_order int         default 0                 null comment '排序',
    is_cover   tinyint(1)  default 0                 null comment '是否封面: 0否 1是',
    created_at datetime    default CURRENT_TIMESTAMP null,
    constraint movie_images_ibfk_1
        foreign key (movie_id) references movies (id)
            on delete cascade
)
    comment '电影图片表';

create index idx_movie_id
    on movie_images (movie_id);

create index idx_release_date
    on movies (release_date);

create index idx_status
    on movies (status);

create table if not exists seats
(
    id         bigint auto_increment
        primary key,
    hall_id    bigint                                not null comment '影厅ID',
    row_num    int                                   not null comment '行号',
    col_num    int                                   not null comment '列号',
    seat_type  varchar(10) default 'normal'          null comment '座位类型: normal/vip/couple',
    status     varchar(15) default 'active'          null comment '状态: active/maintenance',
    created_at datetime    default CURRENT_TIMESTAMP null,
    updated_at datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    constraint uk_hall_seat
        unique (hall_id, row_num, col_num),
    constraint seats_ibfk_1
        foreign key (hall_id) references halls (id)
            on delete cascade
)
    comment '座位表';

create table if not exists showtimes
(
    id         bigint auto_increment
        primary key,
    movie_id   bigint                                not null comment '电影ID',
    hall_id    bigint                                not null comment '影厅ID',
    show_date  date                                  not null comment '放映日期',
    show_time  time                                  not null comment '放映时间',
    price      decimal(8, 2)                         not null comment '基础票价',
    status     varchar(20) default 'normal'          null comment '状态: normal/cancelled/sold_out',
    created_at datetime    default CURRENT_TIMESTAMP null,
    updated_at datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    constraint showtimes_ibfk_1
        foreign key (movie_id) references movies (id)
            on delete cascade,
    constraint showtimes_ibfk_2
        foreign key (hall_id) references halls (id)
            on delete cascade
)
    comment '场次表';

create index idx_hall_date
    on showtimes (hall_id, show_date, show_time);

create index idx_movie_date
    on showtimes (movie_id, show_date);

create table if not exists users
(
    id             bigint auto_increment
        primary key,
    username       varchar(50)                              not null comment '用户名',
    password       varchar(255)                             not null comment '密码(BCrypt加密)',
    phone          varchar(20)                              null comment '手机号',
    role           enum ('user', 'admin')                   not null,
    status         varchar(10)   default 'active'            null comment '状态: active/disabled',
    wallet_balance decimal(10, 2) default 1000.00           null comment '虚拟钱包余额(注册送1000)',
    version        int            default 0                 null comment '乐观锁版本号',
    avatar         varchar(255)                             null comment '头像URL',
    created_at     datetime       default CURRENT_TIMESTAMP null,
    updated_at     datetime       default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    constraint username
        unique (username)
)
    comment '用户表';

create table if not exists notifications
(
    id         bigint auto_increment
        primary key,
    user_id    bigint                                not null comment '用户ID',
    title      varchar(200)                          not null comment '通知标题',
    content    text                                  null comment '通知内容',
    type       varchar(10) default 'order'           null comment '类型: order/system',
    status     varchar(10) default 'unread'          null comment '状态: unread/read',
    created_at datetime    default CURRENT_TIMESTAMP null,
    constraint notifications_ibfk_1
        foreign key (user_id) references users (id)
            on delete cascade
)
    comment '通知表';

create index idx_status
    on notifications (status);

create index idx_user_id
    on notifications (user_id);

create table if not exists orders
(
    id           bigint auto_increment
        primary key,
    order_no     varchar(64)                           not null comment '订单号',
    user_id      bigint                                not null comment '用户ID',
    showtime_id  bigint                                not null comment '场次ID',
    total_amount decimal(10, 2)                        not null comment '总金额',
    status       varchar(15) default 'pending'         null comment '状态: pending/paid/refunded/cancelled',
    created_at   datetime    default CURRENT_TIMESTAMP null,
    updated_at   datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    constraint order_no
        unique (order_no),
    constraint orders_ibfk_1
        foreign key (user_id) references users (id)
            on delete cascade,
    constraint orders_ibfk_2
        foreign key (showtime_id) references showtimes (id)
            on delete cascade
)
    comment '订单表';

create table if not exists order_seats
(
    id       bigint auto_increment
        primary key,
    order_id bigint        not null comment '订单ID',
    seat_id  bigint        not null comment '座位ID',
    price    decimal(8, 2) not null comment '座位价格',
    constraint order_seats_ibfk_1
        foreign key (order_id) references orders (id)
            on delete cascade,
    constraint order_seats_ibfk_2
        foreign key (seat_id) references seats (id)
            on delete cascade
)
    comment '订单座位关联表';

create index idx_order_id
    on order_seats (order_id);

create index idx_seat_id
    on order_seats (seat_id);

create index idx_created_at
    on orders (created_at);

create index idx_order_no
    on orders (order_no);

create index idx_status
    on orders (status);

create index idx_user_id
    on orders (user_id);

create index showtime_id
    on orders (showtime_id);

create table if not exists payments
(
    id             bigint auto_increment
        primary key,
    order_id       bigint                                not null comment '订单ID',
    user_id        bigint                                not null comment '用户ID',
    payment_method varchar(10) default 'wallet'          null comment '支付方式: wallet',
    amount         decimal(10, 2)                        not null comment '支付金额',
    status         varchar(10) default 'success'         null comment '状态: success/failed/refunded',
    created_at     datetime    default CURRENT_TIMESTAMP null,
    constraint payments_ibfk_1
        foreign key (order_id) references orders (id)
            on delete cascade,
    constraint payments_ibfk_2
        foreign key (user_id) references users (id)
            on delete cascade
)
    comment '支付记录表';

create index idx_order_id
    on payments (order_id);

create index idx_user_id
    on payments (user_id);

create table if not exists reviews
(
    id         bigint auto_increment
        primary key,
    user_id    bigint                             not null comment '用户ID',
    movie_id   bigint                             not null comment '电影ID',
    order_id   bigint                             null comment '订单ID(可选)',
    rating     int                                not null comment '评分(1-5)',
    content    text                               null comment '评价内容',
    created_at datetime default CURRENT_TIMESTAMP null,
    constraint reviews_ibfk_1
        foreign key (user_id) references users (id)
            on delete cascade,
    constraint reviews_ibfk_2
        foreign key (movie_id) references movies (id)
            on delete cascade,
    constraint reviews_ibfk_3
        foreign key (order_id) references orders (id)
            on delete set null
)
    comment '评价表';

create index idx_movie_id
    on reviews (movie_id);

create index idx_user_id
    on reviews (user_id);

create index order_id
    on reviews (order_id);

create index idx_phone
    on users (phone);

create index idx_username
    on users (username);


