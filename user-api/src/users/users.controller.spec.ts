import { Test, TestingModule } from '@nestjs/testing';
import { UsersController } from './users.controller';
import { UsersService } from './users.service';
import { CreateUserDto } from './dto/create-user.dto';
import { UpdateUserDto } from './dto/update-user.dto';
import { UserRole, UserStatus } from './schemas/user.schema';
import { ConflictException, NotFoundException } from '@nestjs/common';

describe('UsersController', () => {
  let controller: UsersController;
  let mockUsersService: jest.Mocked<UsersService>;

  const mockUser = {
    _id: 'userId123',
    email: 'test@example.com',
    firstName: 'John',
    lastName: 'Doe',
    phone: '+1234567890',
    role: UserRole.USER,
    status: UserStatus.ACTIVE,
    createdAt: new Date(),
    updatedAt: new Date(),
    toObject: jest.fn().mockReturnValue({
      _id: 'userId123',
      email: 'test@example.com',
      firstName: 'John',
      lastName: 'Doe',
      phone: '+1234567890',
      role: UserRole.USER,
      status: UserStatus.ACTIVE,
      createdAt: new Date(),
      updatedAt: new Date(),
    }),
  };

  beforeEach(async () => {
    mockUsersService = {
      create: jest.fn(),
      findByEmail: jest.fn(),
      findById: jest.fn(),
      findAll: jest.fn(),
      update: jest.fn(),
      remove: jest.fn(),
      validatePassword: jest.fn(),
      updateRefreshToken: jest.fn(),
    } as any;

    const module: TestingModule = await Test.createTestingModule({
      controllers: [UsersController],
      providers: [
        {
          provide: UsersService,
          useValue: mockUsersService,
        },
      ],
    }).compile();

    controller = module.get<UsersController>(UsersController);
  });

  it('should be defined', () => {
    expect(controller).toBeDefined();
  });

  describe('register', () => {
    it('should register a new user successfully', async () => {
      const createUserDto: CreateUserDto = {
        email: 'test@example.com',
        password: 'password123',
        firstName: 'John',
        lastName: 'Doe',
      };

      mockUsersService.create.mockResolvedValue(mockUser);

      const result = await controller.register(createUserDto);

      expect(mockUsersService.create).toHaveBeenCalledWith(createUserDto);
      expect(result).toEqual({
        _id: 'userId123',
        email: 'test@example.com',
        firstName: 'John',
        lastName: 'Doe',
        phone: '+1234567890',
        role: UserRole.USER,
        status: UserStatus.ACTIVE,
        createdAt: expect.any(Date),
        updatedAt: expect.any(Date),
      });
      // Should exclude password and refreshToken
      expect(result).not.toHaveProperty('password');
      expect(result).not.toHaveProperty('refreshToken');
    });

    it('should handle service exceptions during registration', async () => {
      const createUserDto: CreateUserDto = {
        email: 'existing@example.com',
        password: 'password123',
        firstName: 'John',
        lastName: 'Doe',
      };

      mockUsersService.create.mockRejectedValue(new ConflictException('Email already exists'));

      await expect(controller.register(createUserDto)).rejects.toThrow(ConflictException);
    });
  });

  describe('findOne', () => {
    it('should return user by id', async () => {
      mockUsersService.findById.mockResolvedValue(mockUser);

      const result = await controller.findOne('userId123');

      expect(mockUsersService.findById).toHaveBeenCalledWith('userId123');
      expect(result).toEqual({
        _id: 'userId123',
        email: 'test@example.com',
        firstName: 'John',
        lastName: 'Doe',
        phone: '+1234567890',
        role: UserRole.USER,
        status: UserStatus.ACTIVE,
        createdAt: expect.any(Date),
        updatedAt: expect.any(Date),
      });
      // Should exclude password and refreshToken
      expect(result).not.toHaveProperty('password');
      expect(result).not.toHaveProperty('refreshToken');
    });

    it('should handle NotFoundException', async () => {
      mockUsersService.findById.mockRejectedValue(new NotFoundException('User not found'));

      await expect(controller.findOne('nonexistentId')).rejects.toThrow(NotFoundException);
    });
  });

  describe('findAll', () => {
    it('should return paginated users for admin', async () => {
      const mockUsers = [mockUser];
      const mockResult = { users: mockUsers, total: 1 };

      mockUsersService.findAll.mockResolvedValue(mockResult);

      const result = await controller.findAll(0, 10, 'test', 'USER', 'ACTIVE');

      expect(mockUsersService.findAll).toHaveBeenCalledWith(0, 10, {
        email: 'test',
        role: 'USER',
        status: 'ACTIVE',
      });
      expect(result).toEqual({
        content: [
          {
            _id: 'userId123',
            email: 'test@example.com',
            firstName: 'John',
            lastName: 'Doe',
            phone: '+1234567890',
            role: UserRole.USER,
            status: UserStatus.ACTIVE,
            createdAt: expect.any(Date),
            updatedAt: expect.any(Date),
          },
        ],
        totalElements: 1,
        totalPages: 1,
        currentPage: 0,
        size: 10,
      });
    });

    it('should handle empty filters', async () => {
      const mockResult = { users: [], total: 0 };
      mockUsersService.findAll.mockResolvedValue(mockResult);

      const result = await controller.findAll();

      expect(mockUsersService.findAll).toHaveBeenCalledWith(0, 10, {});
      expect(result.content).toEqual([]);
      expect(result.totalElements).toBe(0);
    });
  });

  describe('update', () => {
    it('should update user successfully', async () => {
      const updateUserDto: UpdateUserDto = {
        firstName: 'Jane',
        phone: '+0987654321',
      };

      const updatedUser = { ...mockUser, firstName: 'Jane', phone: '+0987654321' };
      mockUsersService.update.mockResolvedValue(updatedUser);

      const result = await controller.update('userId123', updateUserDto, { user: mockUser });

      expect(mockUsersService.update).toHaveBeenCalledWith('userId123', updateUserDto);
      expect(result.firstName).toBe('Jane');
      expect(result.phone).toBe('+0987654321');
      // Should exclude password and refreshToken
      expect(result).not.toHaveProperty('password');
      expect(result).not.toHaveProperty('refreshToken');
    });

    it('should handle NotFoundException during update', async () => {
      const updateUserDto: UpdateUserDto = { firstName: 'Jane' };
      mockUsersService.update.mockRejectedValue(new NotFoundException('User not found'));

      await expect(controller.update('nonexistentId', updateUserDto, { user: mockUser }))
        .rejects.toThrow(NotFoundException);
    });
  });

  describe('remove', () => {
    it('should remove user successfully', async () => {
      mockUsersService.remove.mockResolvedValue(undefined);

      const result = await controller.remove('userId123', { user: mockUser });

      expect(mockUsersService.remove).toHaveBeenCalledWith('userId123');
      expect(result).toBeUndefined();
    });

    it('should handle NotFoundException during removal', async () => {
      mockUsersService.remove.mockRejectedValue(new NotFoundException('User not found'));

      await expect(controller.remove('nonexistentId', { user: mockUser }))
        .rejects.toThrow(NotFoundException);
    });
  });

  describe('login', () => {
    it('should return placeholder message for login', async () => {
      const loginUserDto = { email: 'test@example.com', password: 'password123' };

      const result = await controller.login(loginUserDto);

      expect(result).toEqual({
        message: 'Login endpoint - to be implemented with AuthService',
      });
    });
  });

  describe('getProfile', () => {
    it('should return current user profile', () => {
      const req = { user: mockUser };

      const result = controller.getProfile(req);

      expect(result).toEqual(mockUser);
    });
  });
});